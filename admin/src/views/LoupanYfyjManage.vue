<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">一房一价</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理每套房源的备案价和销售价</p></div>
      <div class="flex gap-2">
        <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建房源</t-button>
        <t-button theme="warning" variant="outline" @click="openAiDialog"><Sparkles class="w-4 h-4 mr-1" />AI新建房源</t-button>
      </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索楼栋号/房号" clearable class="w-[200px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-input-number v-model="filterLoupanId" placeholder="楼盘ID" :min="0" class="w-[140px]" @enter="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId=null;search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #houseStatus="{ row }">
          <t-tag :theme="row.houseStatus===0?'default':row.houseStatus===1?'warning':row.houseStatus===2?'success':row.houseStatus===3?'danger':'primary'" size="small">
            {{ ['未售','认购','已售','抵押','保留'][row.houseStatus]||'未知' }}
          </t-tag>
        </template>
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑房源':'新建房源'" size="450px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="户型ID"><t-input-number v-model="form.huxingId" :min="0" /></t-form-item>
        <div class="grid grid-cols-3 gap-3">
          <t-form-item label="楼栋号"><t-input v-model="form.buildingNo" /></t-form-item>
          <t-form-item label="楼层"><t-input-number v-model="form.floorNo" :min="1" /></t-form-item>
          <t-form-item label="房号"><t-input v-model="form.roomNo" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="建筑面积(㎡)"><t-input-number v-model="form.area" :min="0" :decimalPlaces="2" /></t-form-item>
          <t-form-item label="朝向"><t-input v-model="form.orientation" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="备案单价(元/㎡)"><t-input-number v-model="form.recordUnitPrice" :min="0" /></t-form-item>
          <t-form-item label="备案总价(元)"><t-input-number v-model="form.recordTotalPrice" :min="0" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="销售单价(元/㎡)"><t-input-number v-model="form.saleUnitPrice" :min="0" /></t-form-item>
          <t-form-item label="销售总价(元)"><t-input-number v-model="form.saleTotalPrice" :min="0" /></t-form-item>
        </div>
        <t-form-item label="房源状态">
          <t-select v-model="form.houseStatus" :options="[{label:'未售',value:0},{label:'认购',value:1},{label:'已售',value:2},{label:'抵押',value:3},{label:'保留',value:4}]" />
        </t-form-item>
        <t-form-item label="备注"><t-textarea v-model="form.remark" :maxlength="500" /></t-form-item>
        <t-form-item label="排序"><t-input-number v-model="form.sort" :min="0" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>

    <t-dialog v-model:visible="aiVisible" header="AI 新建房源" width="680px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-4">
        <t-alert theme="info" message="上传一房一价表图片，AI 自动识别多套房源信息并批量创建。" />
        <t-form label-align="top"><t-form-item label="关联楼盘ID"><t-input-number v-model="aiLoupanId" :min="1" placeholder="所有识别的房源将关联到此楼盘" /></t-form-item></t-form>
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
        <div class="flex justify-center"><t-button theme="primary" size="large" :loading="aiParsing" @click="startAiParse" :disabled="aiTab==='upload' ? aiFiles.length===0 : aiPasteFiles.length===0"><Sparkles class="w-4 h-4 mr-1" />{{ aiParsing?'识别中...':'开始识别' }}</t-button></div>
        <div v-if="aiResult" class="border rounded-lg p-4 bg-gray-50 max-h-[400px] overflow-y-auto">
          <h3 class="font-semibold mb-2">识别到 {{ aiResult.yfyjList?.length||0 }} 套房源</h3>
          <t-collapse class="mb-3"><t-collapse-panel header="OCR 原始文本"><pre class="text-xs text-gray-600 whitespace-pre-wrap max-h-[200px] overflow-y-auto">{{ aiResult.ocrText }}</pre></t-collapse-panel></t-collapse>
          <t-checkbox-group v-model="aiSelected">
            <div v-for="(item,i) in aiResult.yfyjList" :key="i" class="flex items-center gap-3 p-3 border rounded-lg mb-2 bg-white">
              <t-checkbox :value="i" />
              <div class="flex-1 text-sm"><span class="font-bold">{{ item.buildingNo||'' }} {{ item.roomNo||'' }}</span>
                <span class="text-gray-400 ml-2">{{ item.floorNo }}F</span>
                <span class="text-gray-400 ml-2">{{ item.area }}㎡</span>
                <span class="text-gray-400 ml-2" v-if="item.recordUnitPrice">备案{{ item.recordUnitPrice }}元</span>
              </div>
            </div>
          </t-checkbox-group>
          <div class="flex justify-end gap-2 pt-2 border-t mt-3">
            <t-button variant="outline" @click="aiVisible=false">关闭</t-button>
            <t-button theme="primary" :disabled="aiSelected.length===0" :loading="aiSaving" @click="batchCreateYfyj"><Check class="w-4 h-4 mr-1" />批量创建({{ aiSelected.length }})</t-button>
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

const initForm = () => ({ loupanId:null,huxingId:null,buildingNo:'',floorNo:1,roomNo:'',area:0,recordUnitPrice:0,recordTotalPrice:0,saleUnitPrice:null,saleTotalPrice:null,houseStatus:0,orientation:'',remark:'',sort:0 })
const form = reactive(initForm())

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'loupanId',title:'楼盘ID',width:70},
  {colKey:'huxingId',title:'户型ID',width:70},
  {colKey:'buildingNo',title:'楼栋',width:80},
  {colKey:'floorNo',title:'楼层',width:60},
  {colKey:'roomNo',title:'房号',width:80},
  {colKey:'area',title:'面积(㎡)',width:80},
  {colKey:'recordUnitPrice',title:'备案单价',width:90},
  {colKey:'recordTotalPrice',title:'备案总价',width:100},
  {colKey:'houseStatus',title:'状态',width:70},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterLoupanId.value)p.loupanId=filterLoupanId.value;const r=await request.get('/admin/yfyj',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/yfyj/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/yfyj',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/yfyj/${id}`);MessagePlugin.success('已删除');fetchData()}

// ===== AI 新建房源 =====
const aiVisible = ref(false); const aiTab = ref('upload')
const aiFiles = ref([]); const aiPasteFiles = ref([]); const aiLoupanId = ref(null)
const aiParsing = ref(false); const aiSaving = ref(false); const aiResult = ref(null); const aiSelected = ref([])
function openAiDialog(){ aiFiles.value=[]; aiPasteFiles.value=[]; aiResult.value=null; aiSelected.value=[]; aiTab.value='upload'; aiVisible.value=true }
function aiUploadDummy(){ return Promise.resolve({status:'success',response:{}}) }
function onPaste(e) {
  const items = e.clipboardData?.items; if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) { const blob = item.getAsFile(); aiPasteFiles.value.push({ blob, url: URL.createObjectURL(blob) }) }
  }
}
async function startAiParse(){
  const fd = new FormData()
  if (aiTab.value === 'upload') {
    if (!aiFiles.value.length) { MessagePlugin.warning('请先上传图片'); return }
    aiFiles.value.forEach(f => fd.append('files', f.raw))
  } else {
    if (!aiPasteFiles.value.length) { MessagePlugin.warning('请先粘贴图片'); return }
    aiPasteFiles.value.forEach(f => fd.append('files', f.blob, 'paste.png'))
  }
  aiParsing.value=true; aiResult.value=null
  try{
    aiResult.value=await request.post('/admin/yfyj/ai-parse',fd,{headers:{'Content-Type':'multipart/form-data'},timeout:120000})
    aiSelected.value=aiResult.value?.yfyjList?aiResult.value.yfyjList.map((_,i)=>i):[]
    MessagePlugin.success(`识别到 ${aiResult.value?.yfyjList?.length||0} 套房源`)
  }catch(e){ MessagePlugin.error('AI识别失败：'+(e.response?.data?.msg||e.message)) }
  finally{ aiParsing.value=false }
}
async function batchCreateYfyj(){
  if(!aiLoupanId.value){ MessagePlugin.warning('请先输入关联楼盘ID'); return }
  const selected=aiSelected.value.map(i=>aiResult.value.yfyjList[i])
  if(!selected.length){ MessagePlugin.warning('请至少选择一套房源'); return }
  aiSaving.value=true; let created=0
  for(const item of selected){
    try{
      await request.post('/admin/yfyj',{...item,loupanId:aiLoupanId.value,huxingId:null,sort:0,
        area:item.area?Math.round(Number(item.area)):0})
      created++
    }catch{}
  }
  aiSaving.value=false
  MessagePlugin.success(`成功创建 ${created}/${selected.length} 套房源`)
  if(created>0){ aiVisible.value=false; fetchData() }
}

onMounted(fetchData)
</script>
