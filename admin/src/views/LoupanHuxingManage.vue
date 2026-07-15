<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">楼盘户型</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理楼盘下的户型信息</p></div>
      <div class="flex gap-2">
        <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建户型</t-button>
        <t-button theme="warning" variant="outline" @click="openAiDialog"><Sparkles class="w-4 h-4 mr-1" />AI新建户型</t-button>
      </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索户型名称" clearable class="w-[200px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-input-number v-model="filterLoupanId" placeholder="楼盘ID" :min="0" class="w-[140px]" @enter="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId=null;search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #huxingImage="{ row }">
          <img v-if="row.huxingImage" :src="row.huxingImage" class="w-12 h-12 object-cover rounded border" @error="e => e.target.style.display='none'" />
        </template>
        <template #floorType="{ row }">
          <t-tag size="small">{{ ['','小高层','洋房','叠墅','排屋'][row.floorType]||'未知' }}</t-tag>
        </template>
        <template #isShowHouse="{ row }"><t-tag :theme="row.isShowHouse?'success':'default'" size="small">{{ row.isShowHouse?'有':'无' }}样板间</t-tag></template>
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑户型':'新建户型'" size="450px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="户型名称"><t-input v-model="form.huxingName" /></t-form-item>
        <t-form-item label="户型图">
          <div class="flex flex-col gap-2 w-full">
            <t-upload v-model="huxingFiles" :request-method="uploadHuxingImage" :max="1" accept="image/*" theme="image" @success="onHuxingSuccess" @fail="onHuxingFail" @remove="onHuxingRemove" />
            <t-input v-model="form.huxingImage" placeholder="上传后自动填入，也可手动输入URL" />
          </div>
        </t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="建筑面积(㎡)"><t-input-number v-model="form.area" :min="0" /></t-form-item>
          <t-form-item label="套内面积(㎡)"><t-input-number v-model="form.insideArea" :min="0" /></t-form-item>
        </div>
        <div class="grid grid-cols-4 gap-3">
          <t-form-item label="卧室"><t-input-number v-model="form.roomNum" :min="0" /></t-form-item>
          <t-form-item label="厅"><t-input-number v-model="form.hallNum" :min="0" /></t-form-item>
          <t-form-item label="卫生间"><t-input-number v-model="form.toiletNum" :min="0" /></t-form-item>
          <t-form-item label="阳台"><t-input-number v-model="form.balconyNum" :min="0" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="朝向"><t-input v-model="form.orientation" /></t-form-item>
          <t-form-item label="产品类型">
            <t-select v-model="form.floorType" :options="[{label:'小高层',value:1},{label:'洋房',value:2},{label:'叠墅',value:3},{label:'排屋',value:4}]" />
          </t-form-item>
        </div>
        <div class="grid grid-cols-3 gap-3">
          <t-form-item label="单价(元/㎡)"><t-input-number v-model="form.unitPrice" :min="0" /></t-form-item>
          <t-form-item label="总价起(万)"><t-input-number v-model="form.totalPriceStart" :min="0" /></t-form-item>
          <t-form-item label="总价止(万)"><t-input-number v-model="form.totalPriceEnd" :min="0" /></t-form-item>
        </div>
        <t-form-item label="是否有样板间"><t-switch v-model="form.isShowHouse" :custom-value="[1,0]" /></t-form-item>
        <t-form-item label="户型标签"><t-input v-model="form.tag" placeholder="如：三开间朝南,南北通透" /></t-form-item>
        <t-form-item label="排序"><t-input-number v-model="form.sort" :min="0" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>

    <!-- AI 新建户型对话框 -->
    <t-dialog v-model:visible="aiVisible" header="AI 新建户型" width="680px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-4">
        <t-alert theme="info" message="上传户型资料图片（如户型图、户型列表等），AI 将自动识别并提取多个户型信息。一次可录入多个户型。" />
        
        <t-form label-align="top">
          <t-form-item label="关联楼盘ID">
            <t-input-number v-model="aiLoupanId" :min="1" placeholder="所有识别到的户型将关联到此楼盘" />
          </t-form-item>
        </t-form>

        <t-tabs v-model="aiTab" size="small">
          <t-tab-panel value="upload" label="上传图片">
            <t-upload v-model="aiFiles" :request-method="aiUploadDummy" :max="5" multiple accept="image/*" theme="image" :auto-upload="false" tips="支持 JPG/PNG/WebP，最多 5 张" />
          </t-tab-panel>
          <t-tab-panel value="paste" label="粘贴图片">
            <div class="border-2 border-dashed border-gray-200 rounded-lg p-8 text-center cursor-pointer hover:border-[var(--color-primary)] transition-colors" @paste.prevent="onPaste" tabindex="0">
              <Image class="w-10 h-10 text-gray-300 mx-auto mb-2" />
              <p class="text-sm text-[var(--color-text-tertiary)]">在此区域按 Ctrl+V 粘贴截图</p>
            </div>
            <div v-if="aiPasteFiles.length" class="flex flex-wrap gap-2 mt-3">
              <div v-for="(f,i) in aiPasteFiles" :key="i" class="relative">
                <img :src="f.url" class="w-20 h-20 object-cover rounded border" />
                <span class="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center text-xs cursor-pointer" @click="aiPasteFiles.splice(i,1)">×</span>
              </div>
            </div>
          </t-tab-panel>
        </t-tabs>
        
        <div class="flex justify-center">
          <t-button theme="primary" size="large" :loading="aiParsing" @click="startAiHuxingParse" :disabled="aiTab==='upload' ? aiFiles.length===0 : aiPasteFiles.length===0">
            <Sparkles class="w-4 h-4 mr-1" />{{ aiParsing ? 'AI 识别中...' : '开始识别' }}
          </t-button>
        </div>

        <!-- 结果列表 -->
        <div v-if="aiResult" class="border rounded-lg p-4 bg-gray-50 max-h-[400px] overflow-y-auto">
          <h3 class="font-semibold mb-2">识别到 {{ aiResult.huxings?.length || 0 }} 个户型</h3>
          <t-collapse class="mb-3">
            <t-collapse-panel header="OCR 原始识别文本">
              <pre class="text-xs text-gray-600 whitespace-pre-wrap max-h-[200px] overflow-y-auto">{{ aiResult.ocrText }}</pre>
            </t-collapse-panel>
          </t-collapse>
          <t-checkbox-group v-model="aiSelected">
            <div v-for="(hx,i) in aiResult.huxings" :key="i" class="flex items-center gap-3 p-3 border rounded-lg mb-2 bg-white">
              <t-checkbox :value="i" />
              <div class="flex-1 text-sm space-y-1">
                <div><span class="font-bold">{{ hx.huxingName||'未命名' }}</span>
                  <span class="text-gray-400 ml-2">{{ hx.area }}㎡</span>
                  <span v-if="hx.roomNum" class="text-gray-400 ml-1">{{ hx.roomNum }}室{{ hx.hallNum||0 }}厅{{ hx.toiletNum||0 }}卫</span>
                </div>
                <div class="flex flex-wrap gap-1 text-xs text-gray-500">
                  <span v-if="hx.orientation">{{ hx.orientation }}</span>
                  <span v-if="hx.floorType">{{ ['','小高层','洋房','叠墅','排屋'][hx.floorType] }}</span>
                  <span v-if="hx.unitPrice">{{ hx.unitPrice }}元/㎡</span>
                  <span v-if="hx.totalPriceStart">{{ hx.totalPriceStart }}-{{ hx.totalPriceEnd }}万</span>
                </div>
              </div>
            </div>
          </t-checkbox-group>

          <div class="flex justify-end gap-2 pt-2 border-t mt-3">
            <t-button variant="outline" @click="aiVisible=false">关闭</t-button>
            <t-button theme="primary" :disabled="aiSelected.length===0" :loading="aiSaving" @click="batchCreateHuxings">
              <Check class="w-4 h-4 mr-1" />批量创建选中户型 ({{ aiSelected.length }})
            </t-button>
          </div>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, Sparkles, Check, Image } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterLoupanId = ref(null)
const pg = reactive({current:1,pageSize:10,total:0})

const initForm = () => ({ loupanId:null,huxingName:'',area:0,insideArea:0,roomNum:0,hallNum:0,toiletNum:0,balconyNum:0,orientation:'',floorType:1,unitPrice:null,totalPriceStart:null,totalPriceEnd:null,isShowHouse:0,tag:'',sort:0,huxingImage:'' })
const form = reactive(initForm())
const huxingFiles = ref([])

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'loupanId',title:'楼盘ID',width:70},
  {colKey:'huxingName',title:'户型名称',width:160},
  {colKey:'huxingImage',title:'户型图',width:80},
  {colKey:'area',title:'面积(㎡)',width:80},
  {colKey:'roomNum',title:'居室',width:80},
  {colKey:'orientation',title:'朝向',width:70},
  {colKey:'floorType',title:'产品类型',width:80},
  {colKey:'unitPrice',title:'单价',width:90},
  {colKey:'isShowHouse',title:'样板间',width:80},
  {colKey:'sort',title:'排序',width:60},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterLoupanId.value)p.loupanId=filterLoupanId.value;const r=await request.get('/admin/huxings',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;huxingFiles.value=[];Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;huxingFiles.value=[];Object.assign(form,row);drawer.value=true}

async function uploadHuxingImage(file) {
  const fd = new FormData()
  fd.append('file', file.raw)
  const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  return { status: 'success', response: { url: res.url } }
}
function onHuxingSuccess({ file }) { form.huxingImage = file.response?.url || ''; MessagePlugin.success('上传成功'); huxingFiles.value = [] }
function onHuxingFail() { MessagePlugin.error('上传失败'); huxingFiles.value = [] }
function onHuxingRemove() { huxingFiles.value = [] }

// ===== AI 新建户型 =====
const aiVisible = ref(false)
const aiTab = ref('upload')
const aiFiles = ref([])
const aiPasteFiles = ref([])
const aiLoupanId = ref(null)
const aiParsing = ref(false)
const aiSaving = ref(false)
const aiResult = ref(null)
const aiSelected = ref([])

function openAiDialog() { aiFiles.value = []; aiPasteFiles.value = []; aiResult.value = null; aiSelected.value = []; aiTab.value = 'upload'; aiVisible.value = true }
function aiUploadDummy() { return Promise.resolve({ status: 'success', response: {} }) }

function onPaste(e) {
  const items = e.clipboardData?.items; if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) { const blob = item.getAsFile(); aiPasteFiles.value.push({ blob, url: URL.createObjectURL(blob) }) }
  }
}

async function startAiHuxingParse() {
  const fd = new FormData()
  if (aiTab.value === 'upload') {
    if (aiFiles.value.length === 0) { MessagePlugin.warning('请先上传图片'); return }
    aiFiles.value.forEach(f => fd.append('files', f.raw))
  } else {
    if (aiPasteFiles.value.length === 0) { MessagePlugin.warning('请先粘贴图片'); return }
    aiPasteFiles.value.forEach(f => fd.append('files', f.blob, 'paste.png'))
  }
  aiParsing.value = true; aiResult.value = null
  try {
    const fd = new FormData()
    aiFiles.value.forEach(f => fd.append('files', f.raw))
    const res = await request.post('/admin/huxings/ai-parse', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000
    })
    aiResult.value = res
    aiSelected.value = res?.huxings ? res.huxings.map((_, i) => i) : []
    MessagePlugin.success(`识别到 ${res?.huxings?.length || 0} 个户型`)
  } catch (e) {
    MessagePlugin.error('AI 识别失败：' + (e.response?.data?.msg || e.message))
  } finally { aiParsing.value = false }
}

async function batchCreateHuxings() {
  if (!aiLoupanId.value) { MessagePlugin.warning('请先输入关联楼盘ID'); return }
  const selected = aiSelected.value.map(i => aiResult.value.huxings[i])
  if (selected.length === 0) { MessagePlugin.warning('请至少选择一个户型'); return }
  aiSaving.value = true
  let created = 0
  for (const hx of selected) {
    try {
      await request.post('/admin/huxings', {
        ...hx,
        loupanId: aiLoupanId.value,
        sort: 0,
        area: hx.area ? Math.round(Number(hx.area)) : 0,
        insideArea: hx.insideArea ? Math.round(Number(hx.insideArea)) : 0,
        unitPrice: hx.unitPrice ? Math.round(Number(hx.unitPrice)) : null
      })
      created++
    } catch {}
  }
  aiSaving.value = false
  MessagePlugin.success(`成功创建 ${created}/${selected.length} 个户型`)
  if (created > 0) { aiVisible.value = false; fetchData() }
}

async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/huxings/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/huxings',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/huxings/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(fetchData)
</script>
