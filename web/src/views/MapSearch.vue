<template>
  <div class="map-page">
    <!-- 地图容器 -->
    <div class="relative w-full h-[calc(100vh-var(--header-height))]">
      <!-- 侧边筛选面板开关按钮（全端可见，默认收起） -->
      <button
        class="absolute top-4 left-4 z-20 flex items-center gap-1.5 bg-white rounded-full shadow-lg border border-gray-100 px-3.5 py-2 text-sm font-medium text-[var(--color-text-primary)]"
        @click="showPanel = !showPanel"
      >
        <SlidersHorizontal class="w-4 h-4 text-[var(--color-primary)]" />
        {{ showPanel ? '关闭列表' : '楼盘列表' }}
      </button>

      <!-- 侧边筛选面板（默认收起，点击按钮展开/收起；交互与土拍地图地块列表一致） -->
      <div
        class="absolute top-14 left-4 z-10 w-80 max-w-[calc(100vw-2rem)] bg-white rounded-2xl shadow-lg border border-gray-100 overflow-hidden flex flex-col max-h-[calc(100vh-var(--header-height)-5rem)]"
        :class="showPanel ? 'flex' : 'hidden'"
      >
        <!-- 搜索 -->
        <div class="p-4 border-b border-gray-50">
          <div class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
            <MapIcon class="w-5 h-5 text-[var(--color-primary)]" />地图找房
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
            <div class="w-14 h-14 rounded-lg bg-gray-100 flex-shrink-0 overflow-hidden flex items-center justify-center">
              <Building2 class="w-8 h-8 text-gray-300" />
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="text-sm font-bold text-[var(--color-text-primary)] line-clamp-1">{{ lp.projectName }}</h4>
              <p class="text-xs text-[var(--color-text-tertiary)] mt-0.5"><MapPin class="w-2.5 h-2.5 inline -mt-0.5" />{{ lp.district }}{{ lp.plate ? '·'+lp.plate : '' }}</p>
              <div class="flex items-center gap-2 mt-1">
                <span v-if="lp._price" class="text-xs font-bold text-[var(--color-danger)]">
                  <span v-if="lp._priceLabel" class="text-[10px] font-normal text-[var(--color-text-secondary)] mr-0.5">{{ lp._priceLabel }}</span>{{ lp._price }}元/㎡
                </span>
                <span class="text-xs px-1.5 py-0.5 rounded bg-gray-100 text-[var(--color-text-secondary)]">{{ ['','住宅','公寓','商铺','别墅'][lp.houseType]||'' }}</span>
              </div>
            </div>
            <router-link :to="`/loupan/${lp.encodedId}`" class="flex-shrink-0 text-xs text-[var(--color-primary)] hover:underline mt-1">详情</router-link>
          </div>
        </div>
      </div>

      <!-- 地图 -->
      <div id="amap-container" class="w-full h-full" />
      <!-- 右下角：当前缩放级别 + 图层切换 -->
      <div class="absolute bottom-6 right-4 z-20 flex items-center gap-2">
        <!-- 缩放级别（随地图缩放实时更新） -->
        <div
          v-if="mapReady"
          class="flex items-center gap-1 px-2.5 py-1.5 rounded-lg bg-white/95 backdrop-blur-sm text-xs font-medium text-gray-700 shadow-md border border-gray-200 select-none tabular-nums"
          :title="`当前地图缩放级别：${zoomLevel} 级（3~20 级，数字越大越详细）`"
        >
          <span class="text-[var(--color-text-tertiary)]">缩放级别</span>
          <span class="font-bold text-[var(--color-primary)]">{{ zoomLevel }}</span>
        </div>
        <button
          @click="toggleSatellite"
          class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white text-xs font-medium shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
          :class="showSatellite ? 'text-[#0052D9] border-[#0052D9]' : 'text-gray-700'"
        >
          <component :is="showSatellite ? MapIcon : SatelliteIcon" class="w-4 h-4" />
          {{ showSatellite ? '地图' : '卫星' }}
        </button>
      </div>

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
import { Search, Map as MapIcon, Building2, MapPin, AlertCircle, SlidersHorizontal, Satellite as SatelliteIcon } from 'lucide-vue-next'
import request from '@/utils/request'

// ====== 高德地图 Key（在此处替换为你的 Key） ======
const AMAP_KEY = 'ec9016bfbd481d766643253c1bbe5bc3'

const showPanel = ref(false)
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
let satelliteLayer = null
let roadNetLayer = null
const showSatellite = ref(false)
// 当前地图缩放级别（右下角展示用）
const zoomLevel = ref(12)
// 缩放级别大于该值时，在楼盘名称下方显示户型面积、总价范围
const ZOOM_DETAIL_LEVEL = 14
const showMarkerDetail = ref(false)
// marker 与楼盘数据的对应关系：缩放跨过阈值时只刷新标签，不重建标记（避免触发重新定位）
let markerItems = []

/** 同步右下角显示的缩放级别：高德 2.0 为 3~20 级，缩放动画过程中可能带小数 */
function syncZoom() {
  if (!mapInstance) return
  const z = Number(mapInstance.getZoom())
  zoomLevel.value = Number.isFinite(z) ? Math.round(z * 10) / 10 : ''
  const next = Number.isFinite(z) && z > ZOOM_DETAIL_LEVEL
  if (next !== showMarkerDetail.value) {
    showMarkerDetail.value = next
    updateMarkerLabels()
  }
}

// 楼盘价格候选顺序：高层 → 洋房 → 叠墅 → 排屋。
// 楼盘类型不同价格不同，高层均价为空时回退展示其他产品均价（标签用于区分来源）
const PRICE_KEYS = [
  { key: 'avgUnitPrice', label: '' },
  { key: 'avgUnitPriceYangfang', label: '洋房' },
  { key: 'avgUnitPriceDieshu', label: '叠墅' },
  { key: 'avgUnitPricePaiwu', label: '排屋' }
]

function pickPrice(lp) {
  for (const { key, label } of PRICE_KEYS) {
    const v = Number(lp?.[key])
    if (v > 0) return { value: v, label }
  }
  return null
}

/** 户型面积范围：90-140㎡（只有一个值时降级为 90㎡） */
function formatAreaRange(lp) {
  const min = Number(lp?.areaMin) || 0
  const max = Number(lp?.areaMax) || 0
  if (!min && !max) return ''
  const lo = min || max
  const hi = max || min
  return lo === hi ? `${lo}㎡` : `${lo}-${hi}㎡`
}

/** 总价范围：180-260万（数据库单位为万元） */
function formatTotalPriceRange(lp) {
  const min = Number(lp?.minTotalPrice) || 0
  const max = Number(lp?.maxTotalPrice) || 0
  if (!min && !max) return ''
  const lo = min || max
  const hi = max || min
  return lo === hi ? `${lo}万` : `${lo}-${hi}万`
}

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
    const r = await request.get('/public/loupans', { params: { page: 1, size: 200, salesStatus: '0,1', light: true } })
    // 预计算展示价格（_price/_priceLabel），避免模板与 marker 中重复取数
    loupanList.value = (r?.records || []).map(lp => {
      const p = pickPrice(lp)
      return { ...lp, _price: p ? p.value : null, _priceLabel: p ? p.label : '' }
    })
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
  // 卫星图层（默认不显示，供切换）
  satelliteLayer = new window.AMap.TileLayer.Satellite()
  roadNetLayer = new window.AMap.TileLayer.RoadNet()
  // 交通路况图层（默认显示当前交通情况）
  const trafficLayer = new window.AMap.TileLayer.Traffic()
  mapInstance.add(trafficLayer)

  // 缩放级别：初始化时同步一次，之后随缩放（滚轮/双击/按钮/聚焦楼盘）实时更新
  syncZoom()
  mapInstance.on('zoomend', syncZoom)
  mapInstance.on('zoomchange', syncZoom)

  mapReady.value = true
  addMarkers()
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

/**
 * 生成 marker 标签 HTML
 * 缩放级别 > 14 时在楼盘名称下方追加两行：户型面积范围、总价范围
 */
function buildLabelContent(lp) {
  // 只显示均价（万/㎡）；高层价为空时用洋房/叠墅/排屋均价回退，并带类型前缀
  const priceStr = lp._price
    ? `${lp._priceLabel}${Number((Number(lp._price) / 10000).toFixed(1))}万/㎡`
    : '价格待定'
  const nameRow = `<div style="display:flex;align-items:center;justify-content:center;gap:6px">
          <span style="font-weight:500;max-width:120px;overflow:hidden;text-overflow:ellipsis">${lp.projectName}</span>
          <span style="color:#FFE58F;font-weight:bold;font-size:11px;flex-shrink:0">${priceStr}</span>
        </div>`

  const detailRows = []
  if (showMarkerDetail.value) {
    const area = formatAreaRange(lp)
    if (area) detailRows.push(`<div style="font-size:11px;color:#BAE0FF">户型面积：${area}</div>`)
    const total = formatTotalPriceRange(lp)
    if (total) detailRows.push(`<div style="font-size:11px;color:#FFE58F">总价范围：${total}</div>`)
  }

  const layout = detailRows.length
    ? 'flex-direction:column;align-items:center;gap:2px;line-height:1.35'
    : 'align-items:center;justify-content:center;gap:6px'
  return `<div style="background:#0052D9;color:#fff;padding:3px 8px;border-radius:6px;font-size:12px;white-space:nowrap;box-shadow:0 1px 4px rgba(0,0,0,0.2);display:flex;${layout};border:none;outline:none">
          ${nameRow}
          ${detailRows.join('')}
        </div>`
}

/** 缩放跨过阈值时按当前级别刷新所有标记标签（不重建标记，避免视野被重置） */
function updateMarkerLabels() {
  markerItems.forEach(({ marker, lp }) => {
    marker.setLabel({ content: buildLabelContent(lp), direction: 'top' })
  })
}

function addMarkers() {
  if (!mapInstance || !window.AMap) return
  markers.forEach(m => mapInstance.remove(m))
  markers = []
  markerItems = []

  const list = filteredList.value
  if (!list.length) return

  list.forEach(lp => {
    if (!lp.longitude || !lp.latitude) return
    const marker = new window.AMap.Marker({
      position: [lp.longitude, lp.latitude],
      title: lp.projectName,
      label: {
        content: buildLabelContent(lp),
        direction: 'top'
      }
    })

    marker.on('click', () => {
      activeId.value = lp.id
      // 点击标记：新窗口打开楼盘详情页
      window.open(`/loupan/${lp.encodedId}`, '_blank')
    })

    marker.setMap(mapInstance)
    markers.push(marker)
    markerItems.push({ marker, lp })
  })

  if (markers.length) {
    mapInstance.setFitView(markers)
  }
}

watch(filteredList, addMarkers, { deep: true })
onMounted(fetchData)
</script>

<style>
/* 移除高德地图 marker label 外层容器边框 */
.amap-marker-label {
  border: none !important;
  background: transparent !important;
}
</style>
