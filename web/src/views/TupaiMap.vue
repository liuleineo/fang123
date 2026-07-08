<template>
  <div class="map-page">
    <div class="relative w-full h-[calc(100vh-var(--header-height))]">
      <!-- 侧边筛选面板 -->
      <div class="absolute top-4 left-4 z-10 w-80 max-w-[calc(100vw-2rem)] bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden flex flex-col max-h-[calc(100vh-var(--header-height)-2rem)]">
        <div class="p-4 border-b border-gray-50">
          <div class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
            <Map class="w-5 h-5 text-[var(--color-primary)]" />土拍地图
          </div>
          <t-input v-model="keyword" placeholder="搜索地块名称/宗地编号..." clearable size="small" @enter="filterList" @clear="filterList">
            <template #prefix-icon><Search class="w-3.5 h-3.5" /></template>
          </t-input>
          <div class="flex gap-2 mt-2 flex-wrap">
            <t-select v-model="filterDistrict" placeholder="城区" clearable size="small" class="flex-1 min-w-[90px]" :options="districtOpts" @change="filterList" />
            <t-select v-model="filterDate" placeholder="成交时间" clearable size="small" class="flex-1 min-w-[110px]" :options="dateOpts" @change="filterList" />
          </div>
        </div>

        <!-- 列表 -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="loading" class="flex justify-center py-10"><t-loading size="small" /></div>
          <div v-else-if="!filteredList.length" class="text-center py-10 text-sm text-[var(--color-text-tertiary)]">
            <MapPin class="w-10 h-10 text-gray-200 mx-auto mb-2" />
            暂无地块信息
          </div>
          <div
            v-for="item in filteredList"
            :key="item.id"
            class="flex items-start gap-3 p-3 border-b border-gray-50 cursor-pointer hover:bg-blue-50/30 transition-colors"
            :class="{ 'bg-blue-50/50': activeId === item.id }"
            @click="focusItem(item)"
          >
            <div class="w-14 h-14 rounded-lg bg-gray-100 flex-shrink-0 overflow-hidden flex items-center justify-center">
              <MapPin class="w-8 h-8 text-gray-300" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="text-sm font-bold text-[var(--color-text-primary)] line-clamp-1">{{ item.landName }}</h4>
              <p class="text-xs text-[var(--color-text-tertiary)] mt-0.5">{{ item.landNo }}</p>
              <div class="flex items-center gap-2 mt-1">
                <span class="text-xs text-[var(--color-text-secondary)]">{{ item.district }}{{ item.plate ? '·'+item.plate : '' }}</span>
                <span v-if="item.dealPrice" class="text-xs font-bold text-[var(--color-danger)]">{{ item.dealPrice }}万</span>
                <span :class="['text-xs px-1.5 py-0.5 rounded', item.landStatus===1?'bg-green-50 text-green-600':'bg-gray-100 text-gray-500']">{{ item.landStatus===1?'已出让':'待出让' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 地图 -->
      <div id="amap-container" class="w-full h-full" />

      <!-- 右上角结果数量 -->
      <div class="absolute top-4 right-4 z-10 bg-white/90 backdrop-blur rounded-lg shadow px-3 py-1.5 text-xs text-[var(--color-text-secondary)]">
        共 <span class="font-bold text-[var(--color-text-primary)]">{{ filteredList.length }}</span> 块地
      </div>

      <!-- 右侧年份时间轴 -->
      <div class="absolute top-1/2 right-4 z-10 -translate-y-1/2 flex flex-col items-center gap-1">
        <div
          v-for="d in dateOpts"
          :key="d.value"
          class="group flex items-center gap-2 cursor-pointer"
          @click="filterDate = filterDate===d.value ? '' : d.value"
        >
          <span
            class="text-xs font-medium transition-all whitespace-nowrap"
            :class="filterDate===d.value ? 'text-[var(--color-primary)] font-bold text-sm' : 'text-gray-400 group-hover:text-gray-600'"
          >{{ d.label }}</span>
          <span
            class="w-2.5 h-2.5 rounded-full border-2 transition-all flex-shrink-0"
            :class="filterDate===d.value ? 'bg-[var(--color-primary)] border-[var(--color-primary)] scale-125' : 'bg-white border-gray-300 group-hover:border-gray-500'"
          />
        </div>
      </div>

      <div v-if="!mapReady && !mapError" class="absolute inset-0 flex items-center justify-center bg-gray-50/80">
        <div class="text-center"><t-loading size="large" text="加载地图中..." /></div>
      </div>
      <div v-if="mapError" class="absolute inset-0 flex items-center justify-center bg-gray-50/80">
        <div class="text-center max-w-sm p-8">
          <AlertCircle class="w-12 h-12 text-[var(--color-warning)] mx-auto mb-4" />
          <p class="text-[var(--color-text-secondary)] text-sm">{{ mapError }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Search, Map, MapPin, AlertCircle } from 'lucide-vue-next'
import request from '@/utils/request'

const AMAP_KEY = 'ec9016bfbd481d766643253c1bbe5bc3'

const keyword = ref('')
const filterDistrict = ref('')
const filterDate = ref('')
const districtOpts = ref([])
const dateOpts = ref([])
const tupaiList = ref([])
const loading = ref(false)
const activeId = ref(null)
const mapReady = ref(false)
const mapError = ref('')

let mapInstance = null
let markers = []

// WGS84/CGCS2000 → GCJ-02 坐标转换
function wgs84ToGcj02(lng, lat) {
  const a = 6378245.0, ee = 0.00669342162296594323
  const PI = Math.PI
  const transformLat = (x, y) => {
    let ret = -100 + 2 * x + 3 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
    ret += (20 * Math.sin(6 * x * PI) + 20 * Math.sin(2 * x * PI)) * 2 / 3
    ret += (20 * Math.sin(y * PI) + 40 * Math.sin(y / 3 * PI)) * 2 / 3
    ret += (160 * Math.sin(y / 12 * PI) + 320 * Math.sin(y * PI / 30)) * 2 / 3
    return ret
  }
  const transformLng = (x, y) => {
    let ret = 300 + x + 2 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
    ret += (20 * Math.sin(6 * x * PI) + 20 * Math.sin(2 * x * PI)) * 2 / 3
    ret += (20 * Math.sin(x * PI) + 40 * Math.sin(x / 3 * PI)) * 2 / 3
    ret += (150 * Math.sin(x / 12 * PI) + 300 * Math.sin(x / 30 * PI)) * 2 / 3
    return ret
  }
  let dlat = transformLat(lng - 105, lat - 35)
  let dlng = transformLng(lng - 105, lat - 35)
  const rad = lat / 180 * PI
  const magic = 1 - ee * Math.sin(rad) * Math.sin(rad)
  const sqrtMagic = Math.sqrt(magic)
  dlat = dlat * 180 / (a * (1 - ee) / (magic * sqrtMagic) * PI)
  dlng = dlng * 180 / (a / sqrtMagic * Math.cos(rad) * PI)
  return [Number(lng) + dlng, Number(lat) + dlat]
}

const filteredList = computed(() => {
  let list = tupaiList.value.filter(item => item.longitude && item.latitude)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(item =>
      item.landName?.toLowerCase().includes(kw) ||
      item.landNo?.toLowerCase().includes(kw)
    )
  }
  if (filterDistrict.value) list = list.filter(item => item.district === filterDistrict.value)
  if (filterDate.value) {
    list = list.filter(item => item.dealDate && String(item.dealDate).substring(0,4) === filterDate.value)
  }
  return list
})

async function fetchData() {
  loading.value = true
  try {
    tupaiList.value = await request.get('/public/tupai-lands') || []
    const districts = [...new Set(tupaiList.value.map(i => i.district).filter(Boolean))].sort()
    districtOpts.value = districts.map(d => ({ label: d, value: d }))
    const dates = [...new Set(tupaiList.value.map(i => String(i.dealDate).substring(0,4)).filter(Boolean))].sort().reverse()
    dateOpts.value = dates.map(d => ({ label: d+'年', value: d }))
    // 默认筛选今年
    const thisYear = String(new Date().getFullYear())
    if (dateOpts.value.find(d => d.value === thisYear)) filterDate.value = thisYear
    await initMap()
  } catch {} finally { loading.value = false }
}

function filterList() {}

function focusItem(item) {
  activeId.value = item.id
  if (mapInstance && item.longitude && item.latitude) {
    mapInstance.setZoomAndCenter(16, wgs84ToGcj02(item.longitude, item.latitude))
  }
}

async function initMap() {
  if (!AMAP_KEY) { mapError.value = '未配置高德地图 Key'; return }
  if (!window.AMap) {
    await new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
      script.onload = resolve
      script.onerror = () => reject(new Error('高德地图加载失败'))
      document.head.appendChild(script)
    })
  }
  mapInstance = new window.AMap.Map('amap-container', { zoom: 11, center: [120.2, 30.27], resizeEnable: true })
  mapReady.value = true
  addMarkers()
}

function addMarkers() {
  if (!mapInstance || !window.AMap) return
  markers.forEach(m => mapInstance.remove(m))
  markers = []
  const list = filteredList.value
  if (!list.length) return

  list.forEach(item => {
    if (!item.longitude || !item.latitude) return
    const pos = wgs84ToGcj02(item.longitude, item.latitude)
    const marker = new window.AMap.Marker({
      position: pos,
      title: item.landName,
      label: {
        content: `<div style="background:#E37318;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px;white-space:nowrap;box-shadow:0 1px 4px rgba(0,0,0,0.2)">${item.landName||item.landNo}</div>`,
        direction: 'top'
      }
    })

    const dealStr = item.dealPrice ? `${item.dealPrice}万` : '待出让'
    const infoWindow = new window.AMap.InfoWindow({
      content: `
        <div style="min-width:180px">
          ${item.locationImage ? `<img src="${item.locationImage}" style="width:100%;height:100px;object-fit:cover;border-radius:6px 6px 0 0;margin:-8px -8px 8px -8px;display:block" />` : ''}
          <div style="padding:${item.locationImage ? '0 8px 8px' : '8px'}">
            <h4 style="margin:0 0 4px;font-size:14px;font-weight:bold">${item.landName}</h4>
            <p style="margin:0;font-size:12px;color:#86909C">${item.landNo}</p>
            <p style="margin:0;font-size:12px;color:#86909C">${item.district}${item.plate?'·'+item.plate:''} | ${item.landArea}㎡</p>
            <p style="margin:2px 0;font-size:13px;color:#E34D59;font-weight:bold">成交：${dealStr}</p>
            <p style="margin:0;font-size:12px;color:#86909C">${item.winnerCompany||''}</p>
            ${item.dealDate ? `<p style="margin:0;font-size:12px;color:#86909C">${item.dealDate}</p>` : ''}
          </div>
        </div>`,
      offset: new window.AMap.Pixel(0, -30)
    })
    marker.on('click', () => { activeId.value = item.id; infoWindow.open(mapInstance, marker.getPosition()) })
    marker.setMap(mapInstance)
    markers.push(marker)
  })
  if (markers.length && !fitDone) { mapInstance.setFitView(markers); fitDone = true }
}

let fitDone = false
watch(filteredList, addMarkers, { deep: true })
onMounted(fetchData)
</script>
