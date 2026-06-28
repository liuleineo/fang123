<template>
  <div class="map-page">
    <!-- 地图容器 -->
    <div class="relative w-full h-[calc(100vh-var(--header-height))]">
      <!-- 侧边筛选/列表面板 -->
      <div class="absolute top-4 left-4 z-10 w-80 max-w-[calc(100vw-2rem)] bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden flex flex-col max-h-[calc(100vh-var(--header-height)-2rem)]">
        <!-- 搜索 -->
        <div class="p-4 border-b border-gray-50">
          <div class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
            <Map class="w-5 h-5 text-[var(--color-primary)]" />地图找房
          </div>
          <t-input v-model="keyword" placeholder="搜索楼盘名称..." clearable size="small" @enter="filterList" @clear="filterList">
            <template #prefix-icon><Search class="w-3.5 h-3.5" /></template>
          </t-input>
          <div class="flex gap-2 mt-2 flex-wrap">
            <t-select v-model="filterDistrict" placeholder="行政区" clearable size="small" class="flex-1 min-w-[90px]" :options="districtOpts" @change="filterList" />
            <t-select v-model="filterType" placeholder="类型" clearable size="small" class="w-[80px]" :options="[{label:'住宅',value:1},{label:'公寓',value:2},{label:'别墅',value:4}]" @change="filterList" />
          </div>
        </div>

        <!-- 列表 -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="loading" class="flex justify-center py-10"><t-loading size="small" /></div>
          <div v-else-if="!filteredList.length" class="text-center py-10 text-sm text-[var(--color-text-tertiary)]">
            <MapPin class="w-10 h-10 text-gray-200 mx-auto mb-2" />
            暂无符合条件的楼盘
          </div>
          <div
            v-for="lp in filteredList"
            :key="lp.id"
            class="flex items-start gap-3 p-3 border-b border-gray-50 cursor-pointer hover:bg-blue-50/30 transition-colors"
            :class="{ 'bg-blue-50/50': activeId === lp.id }"
            @click="focusLoupan(lp)"
          >
            <div class="w-14 h-14 rounded-lg bg-gray-100 flex-shrink-0 overflow-hidden">
              <t-image v-if="lp.coverImage" :src="lp.coverImage" fit="cover" class="w-full h-full" />
              <Building2 v-else class="w-8 h-8 text-gray-300 m-3" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="text-sm font-bold text-[var(--color-text-primary)] line-clamp-1">{{ lp.projectName }}</h4>
              <p class="text-xs text-[var(--color-text-tertiary)] mt-0.5"><MapPin class="w-2.5 h-2.5 inline -mt-0.5" />{{ lp.district }}{{ lp.plate ? '·'+lp.plate : '' }}</p>
              <div class="flex items-center gap-2 mt-1">
                <span v-if="lp.avgUnitPrice" class="text-xs font-bold text-[var(--color-danger)]">{{ lp.avgUnitPrice }}元/㎡</span>
                <span class="text-xs px-1.5 py-0.5 rounded bg-gray-100 text-[var(--color-text-secondary)]">{{ ['','住宅','公寓','商铺','别墅'][lp.houseType]||'' }}</span>
              </div>
            </div>
            <router-link :to="`/loupan/${lp.encodedId}`" class="flex-shrink-0 text-xs text-[var(--color-primary)] hover:underline mt-1">详情</router-link>
          </div>
        </div>
      </div>

      <!-- 地图 -->
      <div id="amap-container" class="w-full h-full" />

      <!-- 未配置 Key 提示 -->
      <div v-if="!mapReady && !mapError" class="absolute inset-0 flex items-center justify-center bg-gray-50/80">
        <div class="text-center">
          <t-loading size="large" text="加载地图中..." />
        </div>
      </div>
      <div v-if="mapError" class="absolute inset-0 flex items-center justify-center bg-gray-50/80">
        <div class="text-center max-w-sm p-8">
          <AlertCircle class="w-12 h-12 text-[var(--color-warning)] mx-auto mb-4" />
          <p class="text-[var(--color-text-secondary)] text-sm">{{ mapError }}</p>
          <p class="text-xs text-[var(--color-text-tertiary)] mt-2">请前往 <a href="https://console.amap.com/" target="_blank" class="text-[var(--color-primary)]">高德开放平台</a> 申请 Web端 JS API Key</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { Search, Map, Building2, MapPin, AlertCircle } from 'lucide-vue-next'
import request from '@/utils/request'

// ====== 高德地图 Key（在此处替换为你的 Key） ======
const AMAP_KEY = 'ec9016bfbd481d766643253c1bbe5bc3'

const keyword = ref('')
const filterDistrict = ref('')
const filterType = ref(null)
const districtOpts = ref([])
const loupanList = ref([])
const loading = ref(false)
const activeId = ref(null)
const mapReady = ref(false)
const mapError = ref('')

let mapInstance = null
let markers = []

const filteredList = computed(() => {
  let list = loupanList.value.filter(lp => lp.longitude && lp.latitude)
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(lp => lp.projectName?.toLowerCase().includes(kw))
  }
  if (filterDistrict.value) list = list.filter(lp => lp.district === filterDistrict.value)
  if (filterType.value) list = list.filter(lp => lp.houseType === filterType.value)
  return list
})

async function fetchData() {
  loading.value = true
  try {
    const r = await request.get('/public/loupans', { params: { page: 1, size: 200 } })
    loupanList.value = r?.records || []
    const districts = [...new Set(loupanList.value.map(l => l.district).filter(Boolean))].sort()
    districtOpts.value = districts.map(d => ({ label: d, value: d }))
    await initMap()
  } catch {} finally { loading.value = false }
}

function filterList() { /* computed handles filtering */ }

function focusLoupan(lp) {
  activeId.value = lp.id
  if (mapInstance && lp.longitude && lp.latitude) {
    mapInstance.setZoomAndCenter(16, [lp.longitude, lp.latitude])
  }
}

async function initMap() {
  if (!AMAP_KEY) {
    mapError.value = '未配置高德地图 Key'
    return
  }

  if (!window.AMap) {
    await new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
      script.onload = resolve
      script.onerror = () => reject(new Error('高德地图加载失败'))
      document.head.appendChild(script)
    })
  }

  mapInstance = new window.AMap.Map('amap-container', {
    zoom: 12,
    center: [120.32, 30.31],
    resizeEnable: true
  })

  mapReady.value = true
  addMarkers()
}

function addMarkers() {
  if (!mapInstance || !window.AMap) return
  markers.forEach(m => mapInstance.remove(m))
  markers = []

  const list = filteredList.value
  if (!list.length) return

  list.forEach(lp => {
    if (!lp.longitude || !lp.latitude) return
    const marker = new window.AMap.Marker({
      position: [lp.longitude, lp.latitude],
      title: lp.projectName,
      label: {
        content: `<div style="background:#0052D9;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px;white-space:nowrap;box-shadow:0 1px 4px rgba(0,0,0,0.2)">${lp.projectName}</div>`,
        direction: 'top'
      }
    })

    const priceStr = lp.avgUnitPrice ? `${lp.avgUnitPrice}元/㎡` : '价格待定'
    const typeStr = ['','住宅','公寓','商铺','别墅'][lp.houseType] || ''
    const infoWindow = new window.AMap.InfoWindow({
      content: `
        <div style="padding:8px;min-width:160px">
          <div style="display:flex;gap:8px;align-items:flex-start">
            ${lp.coverImage ? `<img src="${lp.coverImage}" style="width:60px;height:45px;border-radius:6px;object-fit:cover" />` : ''}
            <div>
              <h4 style="margin:0 0 4px;font-size:14px;font-weight:bold">${lp.projectName}</h4>
              <p style="margin:0;font-size:12px;color:#86909C">${lp.district}·${lp.plate||''} ${typeStr}</p>
              <p style="margin:2px 0;font-size:13px;color:#E34D59;font-weight:bold">${priceStr}</p>
              <p style="margin:0;font-size:12px;color:#86909C">${lp.areaMin && lp.areaMax ? `面积范围：${lp.areaMin}㎡-${lp.areaMax}㎡` : ''}</p>

              <a href="/loupan/${lp.encodedId}" style="font-size:12px;color:#0052D9;text-decoration:none">查看详情 →</a>
            </div>
          </div>
        </div>`,
      offset: new window.AMap.Pixel(0, -30)
    })

    marker.on('click', () => {
      activeId.value = lp.id
      infoWindow.open(mapInstance, marker.getPosition())
    })

    marker.setMap(mapInstance)
    markers.push(marker)
  })

  if (markers.length) {
    mapInstance.setFitView(markers)
  }
}

watch(filteredList, addMarkers, { deep: true })
onMounted(fetchData)
</script>
