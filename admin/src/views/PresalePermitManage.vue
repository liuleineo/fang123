<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">预售证管理</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理楼盘预售许可证信息</p></div>
      <div class="flex gap-2">
        <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建预售证</t-button>
        <t-button theme="warning" variant="outline" @click="openAiDialog"><Sparkles class="w-4 h-4 mr-1" />AI新建预售证</t-button>
      </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索项目名称/预售证编号/开发公司" clearable class="w-[280px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-input-number v-model="filterLoupanId" placeholder="楼盘ID" :min="0" class="w-[140px]" @enter="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId=null;search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑预售证':'新建预售证'" size="500px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="项目名称"><t-input v-model="form.projectName" /></t-form-item>
        <t-form-item label="预售许可证编号"><t-input v-model="form.permitNo" /></t-form-item>
        <t-form-item label="预售证编号STR"><t-input v-model="form.permitNoStr" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="开发公司"><t-input v-model="form.developCompany" /></t-form-item>
          <t-form-item label="公示结束日期"><t-date-picker v-model="form.publicityEndDate" /></t-form-item>
        </div>
        <t-form-item label="坐落位置"><t-input v-model="form.location" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="销售部地址"><t-input v-model="form.saleAddress" /></t-form-item>
          <t-form-item label="销售部电话"><t-input v-model="form.salePhone" /></t-form-item>
        </div>
        <t-form-item label="网上预(销)售总面积(㎡)"><t-input-number v-model="form.onlineSaleArea" :min="0" :decimalPlaces="2" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>

    <!-- AI 新建预售证 -->
    <t-dialog v-model:visible="aiVisible" header="AI 新建预售证" width="640px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-4">
        <t-alert theme="info" message="上传预售证图片或粘贴图片，AI 自动识别预售证信息并填入表单。" />
        <t-tabs v-model="aiTab" size="small">
          <t-tab-panel value="upload" label="上传图片">
            <t-upload v-model="aiFiles" :request-method="aiUploadDummy" :max="5" multiple accept="image/*" theme="image" :auto-upload="false" tips="支持 JPG/PNG/WebP，最多 5 张" />
          </t-tab-panel>
          <t-tab-panel value="paste" label="粘贴图片">
            <div
              class="border-2 border-dashed border-gray-200 rounded-lg p-8 text-center cursor-pointer hover:border-[var(--color-primary)] transition-colors"
              @paste.prevent="onPaste"
              tabindex="0"
            >
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
          <t-button theme="primary" size="large" :loading="aiParsing" @click="startAiParse" :disabled="aiTab==='upload' ? aiFiles.length===0 : aiPasteFiles.length===0">
            <Sparkles class="w-4 h-4 mr-1" />{{ aiParsing?'识别中...':'开始识别' }}
          </t-button>
        </div>
        <div v-if="aiResult" class="border rounded-lg p-4 bg-gray-50 max-h-[400px] overflow-y-auto">
          <h3 class="font-semibold mb-3">识别结果</h3>
          <t-collapse class="mb-3"><t-collapse-panel header="OCR 原始文本"><pre class="text-xs text-gray-600 whitespace-pre-wrap max-h-[200px] overflow-y-auto">{{ aiResult.ocrText }}</pre></t-collapse-panel></t-collapse>
          <div v-if="aiResult.fields" class="grid grid-cols-2 gap-2 text-sm">
            <div v-if="aiResult.fields.projectName"><span class="text-gray-500">项目名称：</span><span class="font-medium">{{ aiResult.fields.projectName }}</span></div>
            <div v-if="aiResult.fields.permitNoStr"><span class="text-gray-500">编号STR：</span><span class="font-medium">{{ aiResult.fields.permitNoStr }}</span></div>
            <div v-if="aiResult.fields.developCompany"><span class="text-gray-500">开发公司：</span><span class="font-medium">{{ aiResult.fields.developCompany }}</span></div>
          </div>
          <div class="flex justify-end gap-2 pt-2 border-t mt-3">
            <t-button variant="outline" @click="aiVisible=false">关闭</t-button>
            <t-button theme="primary" @click="fillFormFromAi">确认并填入表单</t-button>
          </div>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, Sparkles, Image } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterLoupanId = ref(null)
const pg = reactive({current:1,pageSize:10,total:0})

const initForm = () => ({ loupanId:null,projectName:'',publicityEndDate:'',developCompany:'',location:'',saleAddress:'',salePhone:'',onlineSaleArea:0,permitNo:'',permitNoStr:'' })
const form = reactive(initForm())

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'loupanId',title:'楼盘ID',width:70},
  {colKey:'projectName',title:'项目名称',width:200,ellipsis:true},
  {colKey:'permitNoStr',title:'预售证编号',width:160,ellipsis:true},
  {colKey:'developCompany',title:'开发公司',width:160,ellipsis:true},
  {colKey:'location',title:'坐落位置',width:140,ellipsis:true},
  {colKey:'saleAddress',title:'销售部地址',width:140,ellipsis:true},
  {colKey:'salePhone',title:'销售部电话',width:120},
  {colKey:'onlineSaleArea',title:'总面积(㎡)',width:110},
  {colKey:'publicityEndDate',title:'公示结束日期',width:120},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterLoupanId.value)p.loupanId=filterLoupanId.value;const r=await request.get('/admin/presale-permits',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/presale-permits/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/presale-permits',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/presale-permits/${id}`);MessagePlugin.success('已删除');fetchData()}

// ===== AI 新建预售证 =====
const aiVisible = ref(false)
const aiTab = ref('upload')
const aiFiles = ref([])
const aiPasteFiles = ref([])
const aiParsing = ref(false)
const aiResult = ref(null)

function openAiDialog(){ aiVisible.value=true; aiResult.value=null; aiFiles.value=[]; aiPasteFiles.value=[]; aiTab.value='upload' }
function aiUploadDummy(){ return Promise.resolve({status:'success',response:{}}) }

function onPaste(e) {
  const items = e.clipboardData?.items
  if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) {
      const blob = item.getAsFile()
      aiPasteFiles.value.push({ blob, url: URL.createObjectURL(blob) })
    }
  }
}

async function startAiParse() {
  const fd = new FormData()
  if (aiTab.value === 'upload') {
    if (!aiFiles.value.length) { MessagePlugin.warning('请先上传图片'); return }
    aiFiles.value.forEach(f => fd.append('files', f.raw))
  } else {
    if (!aiPasteFiles.value.length) { MessagePlugin.warning('请先粘贴图片'); return }
    aiPasteFiles.value.forEach(f => fd.append('files', f.blob, 'paste.png'))
  }
  aiParsing.value = true; aiResult.value = null
  try {
    aiResult.value = await request.post('/admin/presale-permits/ai-parse', fd, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
    MessagePlugin.success('识别成功')
  } catch (e) {
    MessagePlugin.error('AI识别失败：' + (e.response?.data?.msg || e.message))
  } finally { aiParsing.value = false }
}

function fillFormFromAi() {
  if (!aiResult.value?.fields) return
  Object.assign(form, initForm())
  const f = aiResult.value.fields
  Object.keys(f).forEach(k => { if (f[k] !== null && f[k] !== undefined) form[k] = f[k] })
  // 从 permitNoStr 自动推导 permitNo：杭房预许字（2026）第00455号 → 202600455
  if (form.permitNoStr && !form.permitNo) {
    const y = form.permitNoStr.match(/（(\d{4})）/)
    const n = form.permitNoStr.match(/第(\d+)号/)
    if (y && n) form.permitNo = y[1] + n[1]
  }
  aiVisible.value = false; drawer.value = true; isEdit.value = false
  MessagePlugin.success('已填入AI识别字段，请核对后保存')
}

onMounted(fetchData)
</script>
