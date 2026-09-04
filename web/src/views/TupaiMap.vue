<template>
  <div class="map-page">
    <div class="relative w-full h-[calc(100vh-var(--header-height))]">
      <!-- 侧边筛选面板开关按钮（全端可见，默认收起） -->
      <button
        class="absolute top-4 left-4 z-20 flex items-center gap-1.5 bg-white rounded-full shadow-lg border border-gray-100 px-3.5 py-2 text-sm font-medium text-[var(--color-text-primary)]"
        @click="showPanel = !showPanel"
      >
        <SlidersHorizontal class="w-4 h-4 text-[var(--color-primary)]" />
        {{ showPanel ? '关闭列表' : '地块列表' }}
      </button>

      <!-- 侧边筛选面板（默认收起，点击按钮展开/收起） -->
      <div
        class="absolute top-14 left-4 z-10 w-80 max-w-[calc(100vw-2rem)] bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden flex flex-col max-h-[calc(100vh-var(--header-height)-5rem)]"
        :class="showPanel ? 'flex' : 'hidden'"
      >
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
              <h4 class="text-sm font-bold text-[var(--color-text-primary)] line-clamp-1 flex items-center gap-1.5">
                <span class="truncate">{{ item.landName }}</span>
                <span v-if="item.dealDate" class="text-xs px-1.5 py-0.5 rounded bg-blue-50 text-blue-600 flex-shrink-0">{{ ym(item.dealDate) }}</span>
              </h4>
              <p class="text-xs text-[var(--color-text-tertiary)] mt-0.5 truncate">{{ item.landNo }}</p>
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

      <!-- 动画演示年份月份（屏幕底部居中，动画结束隐藏） -->
      <div v-if="isAnimating && animationYear" class="absolute inset-x-0 bottom-12 z-10 flex justify-center pointer-events-none">
        <div class="anim-year">{{ animationYear }}年{{ animationMonth }}月{{ animationDay }}日</div>
      </div>

      <!-- 缩放级别显示（右下角） -->
      <div class="absolute bottom-28 right-4 z-20 px-2.5 py-1 rounded bg-white/95 text-xs font-medium text-gray-600 shadow-md border border-gray-200">
        级别 {{ currentZoom }}
      </div>

      <!-- 动画演示按钮（右下角） -->
      <button
        @click="startAnimation"
        :disabled="isAnimating || !filteredList.length"
        class="absolute bottom-16 right-4 z-20 flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white text-xs font-medium shadow-md border border-gray-200 hover:bg-gray-50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        :class="isAnimating ? 'text-[#0052D9] border-[#0052D9]' : 'text-gray-700'"
      >
        <Play class="w-4 h-4" />
        {{ isAnimating ? `演示中 ${animationIndex+1}/${animationSorted.length}` : '动画演示' }}
      </button>

      <!-- 卫星地图切换按钮（右下角） -->
      <button
        @click="toggleSatellite"
        class="absolute bottom-6 right-4 z-20 flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white text-xs font-medium shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
        :class="showSatellite ? 'text-[#0052D9] border-[#0052D9]' : 'text-gray-700'"
      >
        <component :is="showSatellite ? MapIcon : SatelliteIcon" class="w-4 h-4" />
        {{ showSatellite ? '地图' : '卫星' }}
      </button>

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
import { Search, Map as MapIcon, MapPin, AlertCircle, SlidersHorizontal, Satellite as SatelliteIcon, Play } from 'lucide-vue-next'
import request from '@/utils/request'

const AMAP_KEY = 'ec9016bfbd481d766643253c1bbe5bc3'

// 自定义地块定位图标（SVG data URI：品牌蓝圆点 + 白描边 + 内芯）
const PIN_ICON = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 34 34">
    <circle cx="17" cy="17" r="16" fill="#0052D9" opacity="0.12"/>
    <circle cx="17" cy="17" r="11.5" fill="#0052D9" stroke="#FFFFFF" stroke-width="2.5"/>
    <circle cx="17" cy="17" r="4.5" fill="#0052D9"/>
  </svg>`)
// 动画演示起始的红色版定位图标（与 PIN_ICON 同构，仅主色不同）
const PIN_RED = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 34 34">
    <circle cx="17" cy="17" r="16" fill="#FF4D4F" opacity="0.12"/>
    <circle cx="17" cy="17" r="11.5" fill="#FF4D4F" stroke="#FFFFFF" stroke-width="2.5"/>
    <circle cx="17" cy="17" r="4.5" fill="#FF4D4F"/>
  </svg>`)

const showPanel = ref(false)
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

// 从 dealDate 提取"年份月份"，如 2025年03月，兼容 2025-03-15 / 20250315 格式
function ym(d) {
  const s = String(d || '')
  const y = s.substring(0, 4)
  if (!y) return ''
  let m = ''
  if (s[4] === '-') m = s.substring(5, 7)
  else m = s.substring(4, 6)
  return m && m !== '00' ? `${y}年${m}月` : `${y}年`
}

let mapInstance = null
let markers = []
let satelliteLayer = null
let roadNetLayer = null
const showSatellite = ref(false)
const currentZoom = ref(13)

// 动画演示
const isAnimating = ref(false)
const animationIndex = ref(0)
const animationSorted = ref([])
const animationYear = ref('')
const animationMonth = ref('')
const animationDay = ref('')
let animationTimer = null

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
    // 默认不筛选年份，展示所有年份地块
    filterDate.value = null
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
  mapInstance = new window.AMap.Map('amap-container', { zoom: 13, center: [120.21, 30.29], resizeEnable: true })
  // 卫星图层（默认不显示，供切换）
  satelliteLayer = new window.AMap.TileLayer.Satellite()
  roadNetLayer = new window.AMap.TileLayer.RoadNet()
  // 缩放级别变化时，按需显示/隐藏地块 label（zoomend 更可靠，缩放结束后触发）
  mapInstance.on('zoomend', syncLabelVisibility)
  mapInstance.on('zoomchange', syncLabelVisibility)
  mapReady.value = true
  addMarkers()
}

// 缩放级别 <= 14 时只显示定位图标（隐藏文字 label），> 14 时显示文字
// 通过给地图容器加 class 配合 CSS 隐藏 .amap-marker-label，最可靠
function syncLabelVisibility() {
  if (!mapInstance) return
  const z = mapInstance.getZoom() || 0
  currentZoom.value = z
  const el = document.getElementById('amap-container')
  if (!el) return
  if (z <= 14) {
    el.classList.add('hide-label')
  } else {
    el.classList.remove('hide-label')
  }
}

// 切换卫星/标准地图
function toggleSatellite() {
  if (!mapInstance) return
  if (showSatellite.value) {
    mapInstance.remove([satelliteLayer, roadNetLayer])
    showSatellite.value = false
  } else {
    mapInstance.add([satelliteLayer, roadNetLayer])
    showSatellite.value = true
  }
}

// 动画演示：按成交时间从早到晚依次显示地块（地图中心不变，默认定位图标经 CSS 滤镜橙→蓝渐变）
function startAnimation() {
  if (!mapInstance || isAnimating.value || !filteredList.value.length) return
  // 按成交时间从早到晚排序
  const sorted = [...filteredList.value].sort((a, b) =>
    String(a.dealDate || '').localeCompare(String(b.dealDate || ''))
  )
  animationSorted.value = sorted
  animationIndex.value = 0
  isAnimating.value = true
  // 隐藏所有 marker，逐个展示，地图中心/缩放保持当前视角不变
  markers.forEach(m => m.setMap(null))
  animationTimer = setInterval(() => {
    const idx = animationIndex.value
    if (idx >= sorted.length) { stopAnimation(); return }
    const item = sorted[idx]
    const m = markers.find(mk => mk._item && mk._item.id === item.id)
    if (m) {
      // 双层图标：红色在下层淡出的同时蓝色上层淡入，实现 红 → 品牌蓝 #0052D9 渐变
      m.setContent(`<div class="anim-marker-icon"><img class="pin-red" src="${PIN_RED}" /><img class="pin-blue" src="${PIN_ICON}" /></div>`)
      m.setMap(mapInstance)
      activeId.value = item.id
    }
    // 屏幕底部显示当前地块年月日
    const ds = String(item.dealDate || '')
    const year = ds.substring(0, 4)
    let month = ''
    let day = ''
    if (ds[4] === '-') { month = ds.substring(5, 7); day = ds.substring(8, 10) }
    else { month = ds.substring(4, 6); day = ds.substring(6, 8) }
    if (year && year !== animationYear.value) animationYear.value = year
    if (month && month !== '00' && month !== animationMonth.value) animationMonth.value = month
    if (day && day !== '00' && day !== animationDay.value) animationDay.value = day
    animationIndex.value = idx + 1
  }, 30)
}

function stopAnimation() {
  if (animationTimer) { clearInterval(animationTimer); animationTimer = null }
  isAnimating.value = false
  animationIndex.value = 0
  animationYear.value = ''
  animationMonth.value = ''
  animationDay.value = ''
  addMarkers() // 重建所有 marker，恢复默认定位图标
}

function addMarkers() {
  if (!mapInstance || !window.AMap) return
  if (isAnimating.value) stopAnimation()
  markers.forEach(m => mapInstance.remove(m))
  markers = []
  const list = filteredList.value
  if (!list.length) return

  list.forEach(item => {
    if (!item.longitude || !item.latitude) return
    const pos = wgs84ToGcj02(item.longitude, item.latitude)
    const labelContent = `<div style="background:#1677FF;color:#fff;padding:2px 8px;border-radius:4px;font-size:12px;white-space:nowrap;box-shadow:0 1px 4px rgba(0,0,0,0.2);border:none;outline:none">${item.landName||item.landNo}${item.dealDate?'（'+ym(item.dealDate)+'）':''}</div>`
    const marker = new window.AMap.Marker({
      position: pos,
      title: item.landName,
      anchor: 'center',
      content: `<img src="${PIN_ICON}" width="34" height="34" style="display:block" />`,
      label: { content: labelContent, direction: 'top', offset: new window.AMap.Pixel(4, -24) }
    })
    marker._labelContent = labelContent
    marker._item = item
    marker._pos = pos

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
  syncLabelVisibility()
}

watch(filteredList, addMarkers, { deep: true })
onMounted(fetchData)
</script>

<style>
/* 缩放级别 <=14 时隐藏地块文字 label（只显示定位图标） */
#amap-container.hide-label .amap-marker-label {
  display: none !important;
}
/* 移除高德地图 marker label 外层容器边框 */
.amap-marker-label {
  border: none !important;
  background: transparent !important;
}
/* 动画演示：定位图标由红色渐变为品牌蓝 #0052D9（红图淡出 + 蓝图淡入） */
.anim-marker-icon {
  position: relative;
  width: 34px;
  height: 34px;
}
.anim-marker-icon img {
  position: absolute;
  left: 0;
  top: 0;
  width: 34px;
  height: 34px;
  display: block;
}
.anim-marker-icon .pin-red {
  animation: redFadeOut 0.9s ease-out forwards;
  transform-origin: center;
}
.anim-marker-icon .pin-blue {
  opacity: 0;
  animation: blueFadeIn 0.9s ease-out forwards;
}
/* 动画演示：屏幕中央年份月份（一行） */
.anim-year {
  font-size: 36px;
  font-weight: 600;
  color: #000;
  line-height: 1;
  white-space: nowrap;
  user-select: none;
  animation: yearFade 0.5s ease-out;
}
@keyframes yearFade {
  0% { transform: scale(1.4); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
@keyframes redFadeOut {
  0% { opacity: 1; transform: scale(1.25); }
  100% { opacity: 0; transform: scale(1); }
}
@keyframes blueFadeIn {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

</style>
