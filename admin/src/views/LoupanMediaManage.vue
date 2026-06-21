<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">媒体素材</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理楼盘图片/视频/VR素材</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建素材</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索素材标题" clearable class="w-[200px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-input-number v-model="filterLoupanId" placeholder="楼盘ID" :min="0" class="w-[140px]" @enter="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId=null;search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #mediaUrl="{ row }">
          <t-image v-if="row.mediaType<5" :src="row.mediaUrl" fit="cover" class="w-16 h-12 rounded border" />
          <a v-else :href="row.mediaUrl" target="_blank" class="text-[var(--color-primary)] text-sm">查看</a>
        </template>
        <template #mediaType="{ row }">
          <t-tag size="small">{{ ['','实景图','样板间','户型图','航拍','短视频','VR'][row.mediaType]||'未知' }}</t-tag>
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

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑素材':'新建素材'" size="450px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="关联户型ID(可选)"><t-input-number v-model="form.huxingId" :min="0" /></t-form-item>
        <t-form-item label="素材类型">
          <t-select v-model="form.mediaType" :options="[{label:'实景图',value:1},{label:'样板间',value:2},{label:'户型图',value:3},{label:'航拍',value:4},{label:'短视频',value:5},{label:'VR',value:6}]" />
        </t-form-item>
        <t-form-item label="素材URL">
          <div class="flex flex-col gap-2 w-full">
            <t-upload
              v-model="uploadFiles"
              :request-method="uploadRequest"
              :max="1"
              accept="image/*,video/*"
              theme="file"
              @success="onUploadSuccess"
              @fail="onUploadFail"
              @remove="onUploadRemove"
            />
            <t-input v-model="form.mediaUrl" placeholder="上传后自动填入，也可手动输入URL" />
          </div>
        </t-form-item>
        <t-form-item label="素材标题"><t-input v-model="form.mediaTitle" /></t-form-item>
        <t-form-item label="排序"><t-input-number v-model="form.sort" :min="0" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterLoupanId = ref(null)
const pg = reactive({current:1,pageSize:10,total:0})
const uploadFiles = ref([])

function onUploadSuccess({ file }) {
  form.mediaUrl = file.response?.url || ''
  MessagePlugin.success('上传成功')
  uploadFiles.value = []
}
function onUploadFail() {
  MessagePlugin.error('上传失败')
  uploadFiles.value = []
}
function onUploadRemove() {
  uploadFiles.value = []
}

const initForm = () => ({ loupanId:null,huxingId:null,mediaType:1,mediaUrl:'',mediaTitle:'',sort:0 })
const form = reactive(initForm())

async function uploadRequest(file) {
  const fd = new FormData()
  fd.append('file', file.raw)
  const res = await request.post('/admin/medias/upload', fd, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return { status: 'success', response: { url: res.url } }
}

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'loupanId',title:'楼盘ID',width:70},
  {colKey:'huxingId',title:'户型ID',width:70},
  {colKey:'mediaUrl',title:'预览',width:100},
  {colKey:'mediaType',title:'类型',width:80},
  {colKey:'mediaTitle',title:'标题',width:140,ellipsis:true},
  {colKey:'sort',title:'排序',width:60},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterLoupanId.value)p.loupanId=filterLoupanId.value;const r=await request.get('/admin/medias',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/medias/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/medias',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/medias/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(fetchData)
</script>
