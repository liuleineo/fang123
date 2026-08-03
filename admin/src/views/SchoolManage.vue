<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">学校</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理学校校区信息</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建学校</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索学校名称/校区名称/校区码/小区" clearable class="w-[280px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-select v-model="filterType" placeholder="学校类型" clearable class="w-[140px]" :options="typeOpts" @change="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterType='';search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="campusCode" hover stripe size="small" @page-change="onPg">
        <template #schoolType="{ row }"><t-tag size="small">{{ row.schoolType||'-' }}</t-tag></template>
        <template #tier="{ row }">
          <t-tag v-if="row.tier" size="small" theme="warning">{{ row.tier }}梯队</t-tag>
          <span v-else class="text-xs text-gray-300">-</span>
        </template>
        <template #schoolLogo="{ row }">
          <t-image v-if="row.schoolLogo" :src="row.schoolLogo" fit="cover" class="w-10 h-10 rounded" />
          <span v-else class="text-xs text-gray-300">无</span>
        </template>
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.campusCode)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑学校':'新建学校'" size="640px" :footer="false">
      <t-form :data="form" label-align="top">
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="校区标识码（唯一）"><t-input v-model="form.campusCode" :disabled="isEdit" placeholder="如：XX001" /></t-form-item>
          <t-form-item label="学校机构标识码"><t-input v-model="form.schoolOrgCode" /></t-form-item>
        </div>
        <t-form-item label="学校机构名称"><t-input v-model="form.schoolOrgName" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="校区名称"><t-input v-model="form.campusName" /></t-form-item>
          <t-form-item label="学校类型"><t-select v-model="form.schoolType" :options="typeOpts" clearable /></t-form-item>
        </div>
        <t-form-item label="学校地址"><t-input v-model="form.schoolAddress" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="联系电话"><t-input v-model="form.contactPhone" /></t-form-item>
          <t-form-item label="举办者类型"><t-input v-model="form.sponsorType" placeholder="非民办/民办" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          
          <t-form-item label="经度"><t-input-number v-model="form.longitude" :decimal-places="7" /></t-form-item>
          <t-form-item label="纬度"><t-input-number v-model="form.latitude" :decimal-places="7" /></t-form-item>
          <t-form-item label="梯队"><t-select v-model="form.tier" clearable placeholder="选择梯队" :options="tierOpts" /></t-form-item>
        </div>
        <t-form-item label="教育行政主管部门"><t-input v-model="form.eduAdminDepartment" /></t-form-item>
        <t-form-item label="学区范围"><t-textarea v-model="form.schoolDistrictScope" :autosize="{minRows:2,maxRows:5}" /></t-form-item>
        <t-form-item label="学校简介"><t-textarea v-model="form.schoolIntro" :autosize="{minRows:2,maxRows:5}" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="对口初中名称"><t-input v-model="form.targetMiddleSchoolName" /></t-form-item>
          <t-form-item label="对口初中编码"><t-input v-model="form.targetMiddleSchoolCode" /></t-form-item>
        </div>
        <t-form-item label="小区名称（逗号分隔）"><t-textarea v-model="form.communityNames" :autosize="{minRows:2,maxRows:4}" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="学区地图图片URL"><t-input v-model="form.districtMapImage" /></t-form-item>
          <t-form-item label="学校Logo URL"><t-input v-model="form.schoolLogo" /></t-form-item>
        </div>
        <t-form-item label="地图围栏">
          <t-input v-model="form.mapFence" class="mb-2" placeholder="直接输入围栏坐标，格式：lng,lat;lng,lat;lng,lat（分号分隔，至少3个点）" />
          <div class="flex items-center gap-2 w-full">
            <t-button size="small" theme="primary" variant="outline" @click="openFenceDrawer"><PenLine class="w-4 h-4 mr-1" />绘制/查看围栏</t-button>
            <span v-if="form.mapFence" class="text-xs text-[var(--color-text-secondary)]">{{ form.mapFence.split(';').length }} 个围栏点</span>
            <span v-else class="text-xs text-[var(--color-text-tertiary)]">未设置围栏</span>
            <t-button v-if="form.mapFence" size="small" variant="text" theme="danger" @click="clearDraw"><Trash2 class="w-4 h-4 mr-1" />清除</t-button>
          </div>
        </t-form-item>
        <t-form-item label="照片URL列表（逗号分隔）"><t-input v-model="form.photos" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>

    <!-- 围栏绘制弹窗 -->
    <t-dialog v-model:visible="fenceDrawerVisible" header="绘制学区围栏" width="90%" :footer="false" :close-on-overlay-click="false">
      <div class="flex flex-col h-[70vh]">
        <div id="fenceMap" class="flex-1 w-full" />
        <div class="flex items-center gap-2 p-2 bg-gray-50 border-t border-gray-100">
          <t-button size="small" theme="primary" @click="startDraw"><PenLine class="w-4 h-4 mr-1" />开始绘制</t-button>
          <t-button size="small" variant="outline" @click="undoDraw">撤销</t-button>
          <t-button size="small" variant="outline" @click="clearDraw"><Trash2 class="w-4 h-4 mr-1" />清除</t-button>
          <span class="text-xs text-[var(--color-text-tertiary)] ml-auto">点击地图画点，双击/右键结束绘制</span>
        </div>
        <div class="flex items-center justify-end gap-2 p-2 border-t border-gray-100">
          <span v-if="form.mapFence" class="text-xs text-[var(--color-text-secondary)] mr-auto">{{ form.mapFence.split(';').length }} 个围栏点</span>
          <t-button size="small" variant="outline" @click="fenceDrawerVisible=false">取消</t-button>
          <t-button size="small" theme="primary" @click="saveFence">保存围栏</t-button>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, PenLine, Trash2 } from 'lucide-vue-next'
import request from '@/utils/request'

const AMAP_KEY = 'ec9016bfbd481d766643253c1bbe5bc3'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const fenceDrawerVisible = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterType = ref('')
const pg = reactive({current:1,pageSize:10,total:0})

const typeOpts = [
  { label: '小学', value: '小学' }, { label: '初中', value: '初中' }, { label: '九年一贯制', value: '九年一贯制' }
]
const tierOpts = [
  { label: '1梯队', value: 1 }, { label: '2梯队', value: 2 }, { label: '3梯队', value: 3 }
]

const initForm = () => ({
  campusCode:'', schoolOrgCode:'', schoolOrgName:'', campusName:'', schoolAddress:'', contactPhone:'',
  schoolType:'', sponsorType:'', tier:null, longitude:null, latitude:null, eduAdminDepartment:'',
  schoolDistrictScope:'', schoolIntro:'', targetMiddleSchoolName:'', targetMiddleSchoolCode:'',
  communityNames:'', districtMapImage:'', mapFence:'', photos:'', schoolLogo:''
})
const form = reactive(initForm())

const cols = [
  {colKey:'campusCode',title:'校区码',width:80},
  {colKey:'schoolOrgName',title:'学校名称',width:180,ellipsis:true},
  {colKey:'campusName',title:'校区名称',width:120,ellipsis:true},
  {colKey:'schoolType',title:'类型',width:100},
  {colKey:'tier',title:'梯队',width:80},
  {colKey:'schoolAddress',title:'地址',width:180,ellipsis:true},
  {colKey:'schoolLogo',title:'Logo',width:60},
  {colKey:'targetMiddleSchoolName',title:'对口初中',width:120,ellipsis:true},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterType.value)p.schoolType=filterType.value;const r=await request.get('/admin/schools',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;pg.pageSize=p.pageSize;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.campusCode;Object.assign(form,row);drawer.value=true}

// 打开围栏全屏绘制弹窗
function openFenceDrawer() {
  fenceDrawerVisible.value = true
  setTimeout(() => initFenceMap(form.longitude ? [Number(form.longitude), Number(form.latitude)] : null), 300)
}
function saveFence() {
  if (!form.mapFence || form.mapFence.split(';').length < 3) {
    MessagePlugin.warning('请先绘制至少3个点的围栏')
    return
  }
  fenceDrawerVisible.value = false
  MessagePlugin.success('围栏已设置')
}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/schools/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/schools',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){MessagePlugin.error(e.response?.data?.msg||'保存失败')}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/schools/${id}`);MessagePlugin.success('已删除');fetchData()}

// ===== 围栏绘制地图 =====
let fenceMap = null
let fenceOverlay = null
let drawPolygon = null
let mouseTool = null
let drawHistory = []

function loadAMap() {
  return new Promise((resolve, reject) => {
    if (window.AMap) return resolve()
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=1.4.15&key=${AMAP_KEY}&plugin=AMap.MouseTool`
    script.onload = resolve
    script.onerror = () => reject(new Error('高德地图加载失败'))
    document.head.appendChild(script)
  })
}

// 解析 mapFence 为坐标数组
function parseFenceStr(str) {
  if (!str) return []
  const pts = []
  str.split(';').forEach(p => {
    const m = p.split(',')
    if (m.length === 2) {
      const lng = parseFloat(m[0]); const lat = parseFloat(m[1])
      if (!isNaN(lng) && !isNaN(lat)) pts.push([lng, lat])
    }
  })
  return pts
}

// 初始化围栏地图（编辑时显示已有围栏）
async function initFenceMap(center) {
  await nextTick()
  try {
    await loadAMap()
    const defaultCenter = [120.217345, 30.241814]
    if (!fenceMap) {
      fenceMap = new window.AMap.Map('fenceMap', {
        zoom: center ? 16 : 11,
        center: center || defaultCenter,
      })
    } else {
      // 已初始化过，重新定位到学校坐标
      fenceMap.setCenter(center || defaultCenter)
      fenceMap.setZoom(center ? 16 : 11)
    }
    renderExistingFence()
  } catch (e) {
    console.error('围栏地图加载失败', e)
  }
}

// 显示已有围栏
function renderExistingFence() {
  clearOverlays()
  const pts = parseFenceStr(form.mapFence)
  if (pts.length >= 3 && fenceMap) {
    fenceOverlay = new window.AMap.Polygon({
      path: pts,
      strokeColor: '#1890ff', strokeWeight: 2, strokeOpacity: 0.8,
      fillColor: '#1890ff', fillOpacity: 0.12,
    })
    fenceMap.add(fenceOverlay)
    fenceMap.setFitView([fenceOverlay], false, [40, 40, 40, 40])
  }
}

function clearOverlays() {
  if (fenceMap) {
    if (fenceOverlay) { fenceMap.remove(fenceOverlay); fenceOverlay = null }
    if (drawPolygon) { fenceMap.remove(drawPolygon); drawPolygon = null }
  }
  drawHistory = []
}

// 开始绘制
function startDraw() {
  if (!fenceMap) { MessagePlugin.warning('地图未加载'); return }
  clearOverlays()
  form.mapFence = ''
  if (!window.AMap.MouseTool) { MessagePlugin.warning('绘制工具未加载'); return }
  mouseTool = new window.AMap.MouseTool(fenceMap)
  mouseTool.polygon({
    strokeColor: '#e64340', strokeWeight: 2, strokeOpacity: 0.8,
    fillColor: '#e64340', fillOpacity: 0.12,
    cursor: 'crosshair'
  })
  mouseTool.on('draw', (e) => {
    drawPolygon = e.obj
    const path = drawPolygon.getPath()
    form.mapFence = path.map(p => `${p.lng.toFixed(6)},${p.lat.toFixed(6)}`).join(';')
  })
  MessagePlugin.success('在地图上点击绘制学区，双击/右键完成')
}

// 撤销（清除绘制的围栏）
function undoDraw() {
  clearOverlays()
  form.mapFence = ''
  MessagePlugin.success('已撤销绘制')
}

// 清除所有
function clearDraw() {
  if (mouseTool) { mouseTool.close(true); mouseTool = null }
  clearOverlays()
  form.mapFence = ''
  MessagePlugin.success('已清除围栏')
}

onMounted(fetchData)
onBeforeUnmount(() => {
  if (mouseTool) mouseTool.close(true)
  if (fenceMap) fenceMap.destroy()
})
</script>
