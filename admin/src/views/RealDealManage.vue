<template>
  <div>
    <div class="mb-6 flex flex-wrap items-center justify-between gap-x-3 gap-y-2">
      <div><h1 class="text-2xl font-bold">真实成交</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理小区真实成交记录</p></div>
      <div class="flex gap-2">
        <t-button theme="warning" variant="outline" @click="openAiCreate"><Sparkles class="w-4 h-4 mr-1" />AI新建成交</t-button>
        <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建成交</t-button>
        <t-button theme="primary" variant="outline" @click="openExcelImport"><FileSpreadsheet class="w-4 h-4 mr-1" />批量导入</t-button>
      </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50 flex-wrap">
        <t-input v-model="keyword" placeholder="搜索小区/房号/行政区/板块" clearable class="w-[260px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-select v-model="filterLoupanId" placeholder="按楼盘筛选" clearable filterable class="w-[200px]" :options="loupanOpts" @change="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId='';search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #dealDate="{ row }"><span>{{ fmtDate(row.dealDate) }}</span></template>
        <template #dealPrice="{ row }"><span class="text-[var(--color-danger)] font-medium">{{ row.dealPrice }}万</span></template>
        <template #dealUnitPrice="{ row }"><span>{{ (row.dealPrice!=null && row.houseArea) ? (Number(row.dealPrice)*10000/Number(row.houseArea)).toFixed(0)+'元/㎡' : '-' }}</span></template>
        <template #houseArea="{ row }"><span>{{ row.houseArea }}㎡</span></template>
        <template #yfyj="{ row }"><span v-if="row.yfyj" class="text-[var(--color-primary)]">{{ row.yfyj }}万</span><span v-else class="text-xs text-gray-300">-</span></template>
        <template #loupanId="{ row }">
          <span v-if="row.loupanId" class="flex flex-col leading-tight">
            <span class="font-medium text-[var(--color-primary)]">{{ row.loupanId }}</span>
            <span class="text-xs text-[var(--color-text-tertiary)] truncate max-w-[120px]">{{ loupanMap[row.loupanId] || '未匹配到楼盘' }}</span>
          </span>
          <span v-else class="text-xs text-gray-300">-</span>
        </template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-dialog v-model:visible="drawer" :header="isEdit?'编辑成交记录':'新建成交记录'" width="560px" :footer="false" :close-on-overlay-click="false">
      <t-form :data="form" label-align="top">
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="成交日期"><t-date-picker v-model="form.dealDate" class="w-full" format="YYYY-MM-DD" value-type="YYYY-MM-DD" /></t-form-item>
          
        </div>
        <div class="grid grid-cols-2 gap-3">
            <t-form-item label="行政区"><t-input v-model="form.district" placeholder="如：钱塘区" /></t-form-item>
          <t-form-item label="板块"><t-input v-model="form.plate" placeholder="如：金沙湖" /></t-form-item>
          
        </div>
        <t-form-item label="小区名称"><t-input v-model="form.communityName" /></t-form-item>
        <t-form-item label="房号"><t-input v-model="form.roomNo" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="房间面积（㎡）"><t-input-number v-model="form.houseArea" :min="0" :decimal-places="2" /></t-form-item>
          <t-form-item label="成交价格（万元）"><t-input-number v-model="form.dealPrice" :min="0" :decimal-places="2" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="一手买入价（万元）"><t-input-number v-model="form.yfyj" :min="0" :decimal-places="2" /></t-form-item>
          <t-form-item label="楼盘ID（匹配loupan表）">
            <t-select v-model="form.loupanId" filterable clearable placeholder="选择或输入楼盘" :options="loupanOpts" class="w-full" />
          </t-form-item>
        </div>
        <t-form-item label="备注（是否带车位等）"><t-input v-model="form.remark" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-dialog>

    <!-- AI 解析成交播报 -->
    <t-dialog v-model:visible="aiVisible" header="AI新建成交" width="620px" :footer="false">
      <div class="space-y-4">
        <div>
          <p class="text-sm text-[var(--color-text-tertiary)] mb-2">粘贴成交播报文本，AI 自动识别成交字段</p>
          <t-textarea v-model="aiText" placeholder="例如：【成交播报】&#10;楼盘地址：潮映华岸府2-1-602&#10;房源面积：302&#10;挂牌价格：2180万&#10;成交价格：1950万带双车位&#10;维护门店：贝壳钱二观潮店&#10;成交日期：8.6" :autosize="{minRows:6,maxRows:10}" />
        </div>
        <t-button block theme="primary" :loading="aiLoading" :disabled="!aiText.trim()" @click="doAiParse">
          <Sparkles class="w-4 h-4 mr-1" />{{ aiLoading ? '解析中...' : '开始识别' }}
        </t-button>

        <div v-if="aiFields" class="border border-gray-100 rounded-lg overflow-hidden">
          <div class="px-3 py-2 bg-gray-50 font-medium text-sm flex items-center gap-2">识别结果 <span class="text-xs font-normal text-[var(--color-text-tertiary)]">可修改后填入表单</span></div>
          <div class="p-3 space-y-2">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">成交日期</label>
                <t-date-picker v-model="aiFields.dealDate" class="w-full" format="YYYY-MM-DD" value-type="YYYY-MM-DD" />
              </div>
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">行政区</label>
                <t-input v-model="aiFields.district" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">板块</label>
                <t-input v-model="aiFields.plate" />
              </div>
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">楼盘ID（匹配loupan表）</label>
                <t-select v-model="aiFields.loupanId" filterable clearable placeholder="选择或输入楼盘" :options="loupanOpts" class="w-full" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">小区名称</label>
                <t-input v-model="aiFields.communityName" />
              </div>
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">房号</label>
                <t-input v-model="aiFields.roomNo" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">房源面积（㎡）</label>
                <t-input-number v-model="aiFields.houseArea" :min="0" :decimal-places="2" class="w-full" />
              </div>
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">成交价格（万元）</label>
                <t-input-number v-model="aiFields.dealPrice" :min="0" :decimal-places="2" class="w-full" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">一手买入价（万元）</label>
                <t-input-number v-model="aiFields.yfyj" :min="0" :decimal-places="2" class="w-full" />
              </div>
              <div></div>
            </div>
            <div>
              <label class="text-xs text-[var(--color-text-tertiary)] block mb-1">备注（是否带车位等）</label>
              <t-input v-model="aiFields.remark" />
            </div>
          </div>
          <div class="flex justify-end gap-2 p-3 border-t border-gray-100">
            <t-button variant="outline" @click="cancelAi">放弃</t-button>
            <t-button theme="primary" @click="confirmAiFill">填入表单</t-button>
          </div>
        </div>
      </div>
    </t-dialog>

    <!-- Excel 批量导入真实成交 -->
    <t-dialog v-model:visible="excelVisible" header="Excel 批量导入真实成交" width="560px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-3">
        <t-alert theme="info" message="上传 Excel 文件（.xlsx/.xls），第一行为表头。必填列：『小区名称』『成交日期』；其余选填：行政区、板块、房号、面积、成交价、一手买入价、楼盘ID、备注。成交日期支持 2026-08-06 等格式。" />
        <div class="text-xs text-[var(--color-text-tertiary)]">模板表头示例：成交日期 | 行政区 | 板块 | 小区名称 | 房号 | 面积 | 成交价 | 一手买入价 | 楼盘ID | 备注</div>
        <label class="flex flex-col items-center justify-center border-2 border-dashed border-gray-200 rounded-lg py-8 cursor-pointer hover:border-[var(--color-primary)] transition-colors">
          <input type="file" accept=".xlsx,.xls" class="hidden" @change="onExcelFileChange" />
          <FileSpreadsheet class="w-10 h-10 text-gray-300 mb-2" />
          <p class="text-sm text-[var(--color-text-secondary)]">{{ excelFile ? excelFile.name : '点击选择 Excel 文件' }}</p>
        </label>
        <div class="flex justify-end gap-2">
          <t-button variant="outline" :disabled="excelImporting" @click="closeExcelImport">取消</t-button>
          <t-button theme="primary" :loading="excelImporting" :disabled="!excelFile" @click="startExcelImport">开始导入</t-button>
        </div>
        <div v-if="excelResult" class="rounded-lg border border-gray-100 bg-gray-50 p-3">
          <div class="flex items-center gap-4 text-sm">
            <span>共 <b class="text-[var(--color-text-primary)]">{{ excelResult.total }}</b> 条</span>
            <span class="text-green-600">成功 <b>{{ excelResult.success }}</b></span>
            <span class="text-red-500">失败 <b>{{ excelResult.failed }}</b></span>
          </div>
          <div v-if="excelResult.errors?.length" class="mt-2 max-h-40 overflow-y-auto">
            <div v-for="(e,i) in excelResult.errors" :key="i" class="text-xs text-red-500 py-0.5">第 {{ e.row }} 行：{{ e.msg }}</div>
          </div>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, Sparkles, FileSpreadsheet } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterLoupanId = ref('')
const pg = reactive({current:1,pageSize:10,total:0})

// ===== 楼盘选项（匹配 loupan.id 便于核对）=====
const loupanOpts = ref([])
const loupanMap = reactive({}) // id -> projectName，用于列表展示楼盘名
async function fetchLoupanOpts(keyword) {
  try {
    const params = keyword ? { keyword } : {}
    const list = await request.get('/admin/loupans/options', { params })
    loupanOpts.value = (list || []).map(l => ({ label: `${l.id} · ${l.projectName}${l.district ? '（'+l.district+'）' : ''}`, value: l.id }))
    ;(list || []).forEach(l => { loupanMap[l.id] = l.projectName })
  } catch {}
}

// ===== AI 解析成交播报 =====
const aiVisible = ref(false)
const aiLoading = ref(false)
const aiText = ref('')
const aiFields = ref(null)

function openAiCreate() {
  aiText.value = ''
  aiFields.value = null
  aiVisible.value = true
}

async function doAiParse() {
  if (!aiText.value.trim()) { MessagePlugin.warning('请先粘贴成交播报文本'); return }
  aiLoading.value = true
  try {
    const r = await request.post('/admin/real-deals/ai-parse', { text: aiText.value })
    aiFields.value = r.fields || {}
    if (r.fields && r.fields.communityName) MessagePlugin.success('解析完成，请核对后填入')
    else MessagePlugin.warning('解析完成，但部分字段未能识别，请手动补充')
  } catch (e) {
    MessagePlugin.error(e.response?.data?.msg || '解析失败')
  } finally {
    aiLoading.value = false
  }
}

function confirmAiFill() {
  const f = aiFields.value || {}
  // 填入表单
  Object.assign(form, {
    communityName: f.communityName || '',
    roomNo: f.roomNo || '',
    houseArea: f.houseArea ?? null,
    dealPrice: f.dealPrice ?? null,
    dealDate: f.dealDate || '',
    district: f.district || '',
    plate: f.plate || '',
    remark: f.remark || '',
    loupanId: f.loupanId ?? null,
    yfyj: f.yfyj ?? null,
  })
  aiVisible.value = false
  isEdit.value = false
  editId.value = null
  drawer.value = true
  MessagePlugin.success('已填入表单，请核对后保存')
}

function cancelAi() {
  aiVisible.value = false
  aiFields.value = null
}

// ===== Excel 批量导入 =====
const excelVisible = ref(false)
const excelFile = ref(null)
const excelImporting = ref(false)
const excelResult = ref(null)
function openExcelImport() {
  excelFile.value = null
  excelResult.value = null
  excelVisible.value = true
}
function closeExcelImport() {
  if (excelImporting.value) return
  excelVisible.value = false
  excelFile.value = null
  excelResult.value = null
}
function onExcelFileChange(e) {
  excelFile.value = e.target.files?.[0] || null
  excelResult.value = null
}
async function startExcelImport() {
  if (!excelFile.value) { MessagePlugin.warning('请先选择 Excel 文件'); return }
  excelImporting.value = true
  try {
    const fd = new FormData()
    fd.append('file', excelFile.value)
    const res = await request.post('/admin/real-deals/excel-import', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
    excelResult.value = res
    MessagePlugin.success(`导入完成：成功 ${res?.success || 0} 条，失败 ${res?.failed || 0} 条`)
    if (res?.success > 0) fetchData()
  } catch (e) {
    MessagePlugin.error(e?.response?.data?.msg || e?.message || '导入失败')
  } finally { excelImporting.value = false }
}

const initForm = () => ({
  dealDate:'', district:'', plate:'', communityName:'', roomNo:'',
  houseArea:null, dealPrice:null, remark:'', yfyj:null, loupanId:null
})
const form = reactive(initForm())

// 小区名称变化时，默认按小区名搜索加载楼盘下拉选项，方便核对匹配楼盘ID
watch(() => form.communityName, (val) => {
  const kw = (val || '').trim()
  fetchLoupanOpts(kw || undefined)
})

const cols = [
  {colKey:'dealDate',title:'成交日期',width:110},
  {colKey:'district',title:'行政区',width:90},
  {colKey:'plate',title:'板块',width:90},
  {colKey:'communityName',title:'小区名称',width:140,ellipsis:true},
  {colKey:'roomNo',title:'房号',width:80,ellipsis:true},
  {colKey:'houseArea',title:'面积',width:90},
  {colKey:'dealUnitPrice',title:'成交单价',width:110},
  {colKey:'dealPrice',title:'成交价',width:100},
  {colKey:'yfyj',title:'一手价',width:100},
  {colKey:'loupanId',title:'楼盘ID',width:160},
  {colKey:'remark',title:'备注',width:120,ellipsis:true},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmtDate(d){if(!d)return'';const s=String(d);return s.slice(0,10)}

async function fetchData() {
  loading.value=true
  try{
    const p={page:pg.current,size:pg.pageSize}
    if(keyword.value)p.keyword=keyword.value
    if(filterLoupanId.value)p.loupanId=filterLoupanId.value
    const r=await request.get('/admin/real-deals',{params:p})
    data.value=r.records||[];pg.total=r.total||0
  }catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;pg.pageSize=p.pageSize;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{
    if(isEdit.value){await request.put(`/admin/real-deals/${editId.value}`,form);MessagePlugin.success('已更新')}
    else{await request.post('/admin/real-deals',form);MessagePlugin.success('已创建')}
    drawer.value=false;fetchData()
  }catch(e){MessagePlugin.error(e.response?.data?.msg||'保存失败')}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/real-deals/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(() => { fetchData(); fetchLoupanOpts() })
</script>
