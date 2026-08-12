<template>
  <div class="school-map-page flex flex-col h-[calc(100vh-var(--header-height))]">
    <!-- 搜索/筛选栏 -->
    <div class="bg-white border-b border-gray-50 px-3 md:px-4 py-2 flex flex-wrap items-center gap-2 z-50">
      <t-input v-model="keyword" placeholder="搜索学校名称/校区/小区" clearable size="small" class="w-full md:w-[220px]" @enter="doSearch" @clear="doSearch">
        <template #prefix-icon><Search class="w-4 h-4 text-[var(--color-text-tertiary)]" /></template>
      </t-input>
      <div class="flex flex-wrap items-center gap-2 w-full md:w-auto">
        <t-select v-model="filterType" placeholder="学校类型" clearable multiple size="small" class="w-full md:w-[220px]" :options="typeOpts" @change="doSearch" />
        <t-select v-model="filterTier" placeholder="梯队" clearable size="small" class="w-[32%] md:w-[110px]" :options="tierOpts" @change="doSearch" />
        <t-select v-model="filterDept" placeholder="行政区" clearable size="small" class="w-[32%] md:w-[130px]" :options="deptOpts" @change="doSearch" />
      </div>
      <div class="flex items-center gap-2 w-full md:w-auto">
        <span class="text-xs text-[var(--color-text-tertiary)]">共 {{ total }} 所学校</span>
        <t-button size="small" variant="text" @click="reset"><RefreshCw class="w-3.5 h-3.5 mr-1" />重置</t-button>
      </div>
    </div>

    <!-- 地图 -->
    <div class="flex-1 relative">
      <div id="mapContainer" class="absolute inset-0" />
      <!-- 侧边栏：学校列表（手机端隐藏） -->
      <div class="hidden md:flex absolute top-3 left-3 bottom-3 w-[320px] bg-white rounded-xl shadow-lg overflow-hidden flex-col z-30">
        <div class="px-3 py-2 border-b border-gray-50 text-sm font-bold">学校列表 <span class="text-xs font-normal text-[var(--color-text-tertiary)]">{{ schoolList.length }}所</span></div>
        <div class="flex-1 overflow-y-auto">
          <div v-if="loading" class="flex justify-center py-10"><t-loading /></div>
          <div v-else-if="!schoolList.length" class="text-center py-10 text-[var(--color-text-tertiary)] text-sm">暂无学校</div>
          <div v-else>
            <div v-for="s in schoolList" :key="s.campusCode" class="px-3 py-2.5 border-b border-gray-50 hover:bg-[#F7F9FC] cursor-pointer transition-colors" @click="focusSchool(s)">
              <div class="flex items-center gap-2">
                <span class="flex-shrink-0 w-1.5 h-1.5 rounded-full" :class="typeColor(s.schoolType)"></span>
                <span class="font-medium text-sm text-[var(--color-text-primary)] flex-1 truncate">{{ s.schoolOrgName }}</span>
              </div>
              <div class="text-xs text-[var(--color-text-tertiary)] mt-0.5 pl-3.5">{{ s.campusName || '' }}<template v-if="s.schoolType"> · {{ s.schoolType }}</template></div>
              <div v-if="s.schoolAddress" class="text-xs text-[var(--color-text-tertiary)] mt-0.5 pl-3.5 truncate"><MapPin class="w-3 h-3 inline -mt-0.5 mr-0.5" />{{ s.schoolAddress }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { Search, MapPin, RefreshCw } from 'lucide-vue-next'
import request from '@/utils/request'

const AMAP_KEY = 'ec9016bfbd481d766643253c1bbe5bc3'

const keyword = ref('')
const filterType = ref(['小学','九年一贯制'])
const filterTier = ref(null)
const filterDept = ref('')
const schoolList = ref([])
const loading = ref(false)
const total = ref(0)
const deptOpts = ref([])

const typeOpts = [
  { label: '小学', value: '小学' }, { label: '初中', value: '初中' }, { label: '九年一贯制', value: '九年一贯制' },
  { label: '十二年一贯制', value: '十二年一贯制' }, { label: '完全中学', value: '完全中学' }
]

const tierOpts = [
  { label: '1梯队', value: 1 }, { label: '2梯队', value: 2 }, { label: '3梯队', value: 3 }
]

let map = null
let markers = []

function typeColor(type) {
  return type.includes('小学') && !type.includes('九年') ? 'bg-green-500' :
    type.includes('初中') && !type.includes('九年') ? 'bg-blue-500' :
    type.includes('九年') ? 'bg-purple-500' : 'bg-orange-500'
}

function loadScript() {
  return new Promise((resolve, reject) => {
    if (window.AMap) return resolve()
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=1.4.15&key=${AMAP_KEY}`
    script.onload = resolve
    script.onerror = () => reject(new Error('高德地图加载失败'))
    document.head.appendChild(script)
  })
}

async function fetchData() {
  loading.value = true
  try {
    const p = {}
    if (keyword.value) p.keyword = keyword.value
    if (filterType.value && filterType.value.length) p.schoolType = filterType.value.join(',')
    if (filterTier.value) p.tier = filterTier.value
    if (filterDept.value) p.eduAdminDepartment = filterDept.value
    const r = await request.get('/public/schools/light', { params: p })
    schoolList.value = r || []
    total.value = schoolList.value.length
    renderMarkers()
  } catch {} finally { loading.value = false }
}

const SHOW_NAME_ZOOM = 13 // 达到该缩放级别后显示学校名称
let isZoomRerender = false // 标记是否为缩放触发的重渲染（避免打断 setFitView）
let isInitial = true // 首次加载标记：保持默认缩放级别，不执行 setFitView

function renderMarkers() {
  if (!map) return
  clearMarkers()
  const zoom = map.getZoom() || 11
  const showName = zoom >= SHOW_NAME_ZOOM
  const pts = []
  schoolList.value.forEach((s) => {
    if (s.longitude != null && s.latitude != null) {
      const pos = [Number(s.longitude), Number(s.latitude)]
      const color = s.tier === 1 ? '#e64340' : s.tier === 2 ? '#ff8c00' : '#1890ff'
      const content = `
        <div style="display:flex;align-items:center;white-space:nowrap;cursor:pointer;filter:drop-shadow(0 2px 3px rgba(0,0,0,.35))">
          <svg viewBox="0 0 24 24" width="34" height="42" style="display:block">
            <path d="M12 2C7.6 2 4 5.6 4 10c0 5.6 8 12 8 12s8-6.4 8-12c0-4.4-3.6-8-8-8z" fill="${color}" stroke="#fff" stroke-width="2" stroke-linejoin="round"/>
            <circle cx="12" cy="10" r="3.2" fill="#fff"/>
          </svg>
          ${showName ? `<span style="background:rgba(255,255,255,.92);color:#333;font-size:11px;font-weight:500;padding:1px 6px;border-radius:3px;box-shadow:0 1px 3px rgba(0,0,0,.2);margin-left:4px;max-width:140px;overflow:hidden;text-overflow:ellipsis">${s.schoolOrgName}</span>` : ''}
        </div>`
      const marker = new window.AMap.Marker({
        position: pos,
        content,
        offset: new window.AMap.Pixel(-17, -42),
        title: s.schoolOrgName,
      })
      marker.on('click', () => {
        showInfo(s)
      })
      pts.push(marker.getPosition())
      markers.push(marker)
    }
  })
  map.add(markers)
  if (pts.length && !isZoomRerender && !isInitial) map.setFitView(markers, false, [70, 70, 70, 70])
  isZoomRerender = false
  isInitial = false
}

function clearMarkers() {
  if (map && markers.length) map.remove(markers)
  markers = []
}

// 解析 map_fence 为坐标数组（过滤重复点/无效点）
function parseFence(fence) {
  if (!fence) return []
  const seen = new Set()
  const pts = []
  fence.split(';').forEach(p => {
    const m = p.split(',')
    if (m.length === 2) {
      const lng = parseFloat(m[0]); const lat = parseFloat(m[1])
      if (isNaN(lng) || isNaN(lat)) return
      const key = `${lng},${lat}`
      if (seen.has(key)) return // 跳过重复点
      seen.add(key)
      pts.push([lng, lat])
    }
  })
  return pts.length >= 3 ? pts : [] // 至少3个点才构成多边形
}

let fenceOverlay = null

function clearFence() {
  if (fenceOverlay && map) {
    map.remove(fenceOverlay)
    fenceOverlay = null
  }
}

// 点击学校后请求完整详情数据并展示弹窗+围栏
async function showInfo(s) {
  // 清除旧围栏
  clearFence()
  const lng = Number(s.longitude), lat = Number(s.latitude)
  let info = s
  // 精简列表不含完整数据，请求详情接口
  try {
    const detail = await request.get(`/public/schools/${s.campusCode}`)
    if (detail) info = detail
  } catch {}
  const fencePts = parseFence(info.mapFence)
  if (fencePts.length >= 3) {
    fenceOverlay = new window.AMap.Polygon({
      path: fencePts,
      strokeColor: '#1890ff',
      strokeWeight: 2,
      strokeOpacity: 0.8,
      fillColor: '#1890ff',
      fillOpacity: 0.12,
    })
    map.add(fenceOverlay)
  }
  const schoolDetailUrl = `https://rxyj.hzedu.gov.cn/#/schoolDetail?year=2026&schoolName=${info.campusCode}&id=0`
  // 包含小区：用逗号/顿号分割，两列展示
  let communityHtml = ''
  if (info.communityNames) {
    const list = String(info.communityNames).split(/[,，、;；]/).map(x => x.trim()).filter(Boolean)
    if (list.length) {
      const rows = []
      for (let i = 0; i < list.length; i += 2) {
        const c1 = `<td style="width:50%;padding:3px 8px;border:1px solid #eee;font-size:11px;color:#555">${list[i]}</td>`
        const c2 = i + 1 < list.length ? `<td style="width:50%;padding:3px 8px;border:1px solid #eee;font-size:11px;color:#555">${list[i+1]}</td>` : '<td style="width:50%;border:1px solid #eee"></td>'
        rows.push(`<tr>${c1}${c2}</tr>`)
      }
      communityHtml = `
        <div style="margin:6px 0">
          <div style="font-weight:500;color:#333;margin-bottom:3px">包含小区（${list.length}个）</div>
          <table style="width:100%;border-collapse:collapse">
            <tbody>${rows.join('')}</tbody>
          </table>
        </div>`
    }
  }
  const content = `
    <div style="font-size:12px;width:280px;max-height:360px;overflow-y:auto;background:#fff;border-radius:8px;box-shadow:0 4px 16px rgba(0,0,0,.15);padding:12px">
      <div style="font-weight:bold;font-size:13px;margin-bottom:4px;padding-right:8px">${info.schoolOrgName}${info.campusName ? '（'+info.campusName+'）':''}</div>
      <div style="color:#666;margin:2px 0">${info.schoolType || ''} ${info.sponsorType ? '· '+info.sponsorType : ''}</div>
      ${info.targetMiddleSchoolName ? `<div style="color:#0052D9;margin-top:3px">对口初中：${info.targetMiddleSchoolName}</div>` : ''}
      ${communityHtml}
      ${fencePts.length >= 3 ? '<div style="color:#1890ff;margin-top:3px">已显示学区范围</div>' : ''}
      <div style="margin-top:6px;padding-top:6px;border-top:1px dashed #eee">
        <a href="${schoolDetailUrl}" target="_blank" rel="noopener" style="color:#1890ff;text-decoration:none;font-weight:500">访问入学早知道 ›</a>
      </div>
    </div>`
  new window.AMap.InfoWindow({ content, isCustom: true, offset: new window.AMap.Pixel(0, -20), autoMove: true }).open(map, [lng, lat])
}

function focusSchool(s) {
  if (!map || s.longitude == null || s.latitude == null) return
  map.setZoomAndCenter(17, [Number(s.longitude), Number(s.latitude)])
  showInfo(s)
}

function doSearch() { fetchData() }
function reset() { keyword.value=''; filterType.value=['小学','九年一贯制']; filterTier.value=null; filterDept.value=''; fetchData() }

async function fetchDepts() {
  try {
    const r = await request.get('/public/schools/light', { params: {} })
    const set = new Set((r||[]).map(s => s.eduAdminDepartment).filter(Boolean))
    deptOpts.value = [...set].map(d => ({ label: d, value: d }))
  } catch {}
}

let lastShowNameState = null

onMounted(async () => {
  await nextTick()
  try {
    await loadScript()
    map = new window.AMap.Map('mapContainer', { zoom: 13, center: [120.217345, 30.241814] })
    map.on('zoomchange', () => {
      const showName = (map.getZoom() || 11) >= SHOW_NAME_ZOOM
      if (lastShowNameState !== showName) {
        lastShowNameState = showName
        isZoomRerender = true
        renderMarkers()
      }
    })
    fetchData()
    fetchDepts()
  } catch (e) {
    console.error('地图加载失败', e)
  }
})

onBeforeUnmount(() => { clearMarkers(); clearFence() })
</script>


