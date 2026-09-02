<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">媒体素材</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理楼盘图片/视频/VR素材</p></div>
      <div class="flex gap-2">
        <t-button theme="primary" variant="outline" @click="openVideoCreate"><Video class="w-4 h-4 mr-1" />新建视频素材</t-button>
        <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建素材</t-button>
        <t-button theme="primary" variant="outline" @click="openBatch"><Layers class="w-4 h-4 mr-1" />批量新建素材</t-button>
      </div>
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
          <t-image v-if="row.mediaType!==5&&row.mediaType!==6" :src="row.mediaUrl" fit="cover" class="w-16 h-12 rounded border" />
          <a v-else :href="row.mediaUrl" target="_blank" class="text-[var(--color-primary)] text-sm">查看</a>
        </template>
        <template #mediaType="{ row }">
          <t-tag size="small">{{ ['','实景图','样板间','户型图','航拍','短视频','VR','设计图','区位图','效果图','施工进度','周边配套'][row.mediaType]||'未知' }}</t-tag>
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

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑素材':(isVideoCreate?'新建视频素材':'新建素材')" size="450px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="关联户型ID(可选)"><t-input-number v-model="form.huxingId" :min="0" /></t-form-item>
        <t-form-item label="素材类型">
          <t-select v-model="form.mediaType" :disabled="isVideoCreate" :options="[{label:'实景图',value:1},{label:'样板间',value:2},{label:'户型图',value:3},{label:'航拍',value:4},{label:'短视频',value:5},{label:'VR',value:6},{label:'设计图',value:7},{label:'区位图',value:8},{label:'效果图',value:9},{label:'施工进度',value:10},{label:'周边配套',value:11}]" />
        </t-form-item>
        <t-form-item label="素材URL">
          <div class="flex flex-col gap-2 w-full">
            <t-tabs>
              <t-tab-panel value="upload" label="上传文件">
                <t-upload
                  v-model="uploadFiles"
                  :request-method="uploadRequest"
                  :max="1"
                  :accept="isVideoCreate ? 'video/*' : 'image/*,video/*'"
                  theme="file"
                  @success="onUploadSuccess"
                  @fail="onUploadFail"
                  @remove="onUploadRemove"
                />
              </t-tab-panel>
              <t-tab-panel value="paste" label="粘贴图片">
                <div class="border-2 border-dashed border-gray-200 rounded-lg p-8 text-center cursor-pointer hover:border-[var(--color-primary)] transition-colors" @paste.prevent="onPaste" tabindex="0">
                  <Image class="w-10 h-10 text-gray-300 mx-auto mb-2" />
                  <p class="text-sm text-[var(--color-text-tertiary)]">在此区域按 Ctrl+V 粘贴截图</p>
                </div>
                <div v-if="pasteFiles.length" class="flex flex-wrap gap-2 mt-3">
                  <div v-for="(f,i) in pasteFiles" :key="i" class="relative">
                    <img :src="f.url" class="w-20 h-20 object-cover rounded border" />
                    <span class="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center text-xs cursor-pointer" @click="pasteFiles.splice(i,1)">×</span>
                  </div>
                </div>
                <t-button v-if="pasteFiles.length" variant="outline" size="small" class="mt-2" :loading="pasteUploading" @click="uploadPasted">上传并填入URL</t-button>
              </t-tab-panel>
            </t-tabs>
            <t-input v-model="form.mediaUrl" placeholder="上传后自动填入，也可手动输入URL" />
          </div>
        </t-form-item>
        <t-form-item label="素材标题"><t-input v-model="form.mediaTitle" /></t-form-item>
        <t-form-item label="排序"><t-input-number v-model="form.sort" :min="0" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>

    <!-- 批量新建素材 -->
    <t-dialog v-model:visible="batchVisible" header="批量新建素材" width="520px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-4">
        <t-form-item label="楼盘ID"><t-input-number v-model="batchForm.loupanId" :min="1" /></t-form-item>
        <t-form-item label="素材类型">
          <t-select v-model="batchForm.mediaType" :options="[{label:'实景图',value:1},{label:'样板间',value:2},{label:'户型图',value:3},{label:'航拍',value:4},{label:'短视频',value:5},{label:'VR',value:6},{label:'设计图',value:7},{label:'区位图',value:8},{label:'效果图',value:9},{label:'施工进度',value:10},{label:'周边配套',value:11}]" />
        </t-form-item>
        <t-form-item label="选择图片（多选）">
          <t-upload
            v-model="batchFiles"
            :request-method="()=>Promise.resolve({status:'success'})"
            :max="20"
            multiple
            accept="image/*"
            theme="file"
            :auto-upload="false"
            tips="支持 JPG/PNG/WebP，最多 20 张"
          />
        </t-form-item>
        <div v-if="batchFiles.length" class="text-sm text-[var(--color-text-secondary)]">已选择 {{ batchFiles.length }} 张图片</div>
        <t-button block theme="primary" :loading="batchUploading" @click="doBatchUpload" :disabled="!batchFiles.length">
          <Upload class="w-4 h-4 mr-1" />{{ batchUploading ? `上传中 ${batchProgress}...` : `批量上传 ${batchFiles.length} 张` }}
        </t-button>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, Image, Layers, Upload, Video } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const isVideoCreate = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterLoupanId = ref(null)
const pg = reactive({current:1,pageSize:10,total:0})
const uploadFiles = ref([])
const uploadTab = ref('upload')
const pasteFiles = ref([])
const pasteUploading = ref(false)

function onPaste(e) {
  const items = e.clipboardData?.items; if (!items) return
  for (const item of items) {
    if (item.type.startsWith('image/')) { const blob = item.getAsFile(); pasteFiles.value.push({ blob, url: URL.createObjectURL(blob) }) }
  }
}
async function uploadPasted() {
  if (!pasteFiles.value.length) return
  pasteUploading.value = true
  try {
    let blob = pasteFiles.value[0].blob
    try { blob = await compressImage(blob) } catch {}
    const fd = new FormData(); fd.append('file', blob, 'image.jpg')
    const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.mediaUrl = res.url
    pasteFiles.value = []
    MessagePlugin.success('上传成功')
  } catch { MessagePlugin.error('上传失败') }
  finally { pasteUploading.value = false }
}



// 前端图片压缩：宽2000px，JPG格式
async function compressImage(file) {
  return new Promise((resolve, reject) => {
    const img = document.createElement('img')
    img.onload = () => {
      const canvas = document.createElement('canvas')
      let w = img.width, h = img.height
      if (w > 2000) { h = Math.round(h * 2000 / w); w = 2000 }
      canvas.width = w; canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, w, h)
      ctx.drawImage(img, 0, 0, w, h)
      canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('compress failed')), 'image/jpeg', 0.85)
    }
    img.onerror = () => reject(new Error('image load failed'))
    img.src = URL.createObjectURL(file)
  })
}

async function uploadRequest(file) {
  let blob = file.raw
  if (blob.type.startsWith('image/') && !blob.type.includes('svg')) {
    try {
      const origSize = (blob.size / 1024).toFixed(1)
      blob = await compressImage(blob)
      const newSize = (blob.size / 1024).toFixed(1)
      console.log(`图片压缩: ${origSize}KB → ${newSize}KB`)
    } catch (e) { console.error('压缩失败:', e) }
  }
  const fd = new FormData()
  fd.append('file', blob, 'image.jpg')
  const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  return { status: 'success', response: { url: res.url } }
}

function onUploadSuccess({ file }) { form.mediaUrl = file.response?.url || ''; MessagePlugin.success('上传成功'); uploadFiles.value = [] }
function onUploadFail() { MessagePlugin.error('上传失败'); uploadFiles.value = [] }
function onUploadRemove() { uploadFiles.value = [] }

const initForm = () => ({ loupanId:null,huxingId:null,mediaType:1,mediaUrl:'',mediaTitle:'',sort:0 })
const form = reactive(initForm())

async function uploadAllFiles() {
  if (!form.loupanId) { MessagePlugin.warning('请先选择楼盘ID'); return }
  batchUploading.value = true
  let created = 0
  for (const f of uploadFiles.value) {
    try {
      const fd = new FormData(); fd.append('file', f)
      const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      await request.post('/admin/medias', { ...form, id: null, mediaUrl: res.url })
      created++
    } catch (e) {
      console.error('upload failed:', e.response?.status, e.response?.data || e.message)
      MessagePlugin.error('上传失败: ' + (e.response?.data?.msg || e.message))
    }
  }
  batchUploading.value = false; uploadFiles.value = []
  MessagePlugin.success(`成功创建 ${created} 条素材`)
  if (created > 0) { drawer.value = false; fetchData() }
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
function onPg(p){pg.current=p.current;pg.pageSize=p.pageSize;fetchData()}
function openCreate(){isEdit.value=false;isVideoCreate.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openVideoCreate(){isEdit.value=false;isVideoCreate.value=true;editId.value=null;Object.assign(form,initForm(),{mediaType:5});drawer.value=true}
function openEdit(row){isEdit.value=true;isVideoCreate.value=false;editId.value=row.id;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/medias/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/medias',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/medias/${id}`);MessagePlugin.success('已删除');fetchData()}

// ===== 批量新建素材 =====
const batchVisible = ref(false)
const batchFiles = ref([])
const batchUploading = ref(false)
const batchProgress = ref('')
const batchForm = reactive({ loupanId: null, mediaType: 1 })

function openBatch() {
  batchForm.loupanId = null
  batchForm.mediaType = 1
  batchFiles.value = []
  batchVisible.value = true
}

async function doBatchUpload() {
  if (!batchForm.loupanId) { MessagePlugin.warning('请先选择楼盘ID'); return }
  if (!batchFiles.value.length) { MessagePlugin.warning('请先选择图片'); return }
  batchUploading.value = true
  let created = 0
  const total = batchFiles.value.length
  for (let i = 0; i < batchFiles.value.length; i++) {
    batchProgress.value = `${i + 1}/${total}`
    try {
      let blob = batchFiles.value[i].raw
      // 前端压缩
      if (blob.type.startsWith('image/') && !blob.type.includes('svg')) {
        const origSize = (blob.size / 1024).toFixed(1)
        blob = await compressImage(blob)
        const newSize = (blob.size / 1024).toFixed(1)
        console.log(`批量图片压缩: ${origSize}KB → ${newSize}KB`)
      }
      // 上传文件
      const fd = new FormData(); fd.append('file', blob, 'image.jpg')
      const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      // 创建素材记录
      await request.post('/admin/medias', { loupanId: batchForm.loupanId, mediaType: batchForm.mediaType, mediaUrl: res.url, mediaTitle: '', sort: 0 })
      created++
    } catch (e) {
      console.error('batch upload failed:', e)
    }
  }
  batchUploading.value = false
  batchFiles.value = []
  batchVisible.value = false
  MessagePlugin.success(`批量创建完成：${created}/${total} 条素材`)
  if (created > 0) fetchData()
}

onMounted(fetchData)
</script>
