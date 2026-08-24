<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">楼盘动态管理</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理楼盘建设/销售/优惠动态信息</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建动态</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50 flex-wrap">
        <t-input v-model="keyword" placeholder="搜索动态标题" clearable class="w-[240px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-input-number v-model="filterLoupanId" placeholder="楼盘ID" :min="0" class="w-[140px]" @enter="search" />
        <t-select v-model="filterType" placeholder="动态类型" clearable style="width:150px" :options="typeOpts" @change="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId=null;filterType=null;search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #type="{ row }"><t-tag size="small" :theme="typeTheme[row.type]||'default'">{{ typeMap[row.type]||'未知' }}</t-tag></template>
        <template #content="{ row }"><span class="text-xs line-clamp-2">{{ row.content }}</span></template>
        <template #images="{ row }">
          <div class="flex gap-1">
            <img v-for="(url,i) in (row.images||'').split(',').filter(Boolean).slice(0,3)" :key="i" :src="url" class="w-10 h-10 object-cover rounded border" @error="e=>e.target.style.display='none'" />
          </div>
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

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑动态':'新建动态'" size="560px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="动态标题"><t-input v-model="form.title" :maxlength="200" /></t-form-item>
        <t-form-item label="动态类型">
          <t-select v-model="form.type" :options="typeOpts" />
        </t-form-item>
        <t-form-item label="动态内容"><t-textarea v-model="form.content" :autosize="{ minRows: 4, maxRows: 8 }" placeholder="动态详细内容" /></t-form-item>
        <t-form-item label="动态图片">
          <div class="flex flex-col gap-2 w-full">
            <t-tabs>
              <t-tab-panel value="upload" label="上传图片">
                <t-upload
                  v-model="uploadFiles"
                  :auto-upload="false"
                  :max="5"
                  multiple
                  accept="image/*"
                  theme="file"
                  tips="选择图片后点击下方按钮上传（支持 JPG/PNG/WebP，最多 5 张）"
                />
                <t-button v-if="uploadFiles.length" theme="primary" size="small" class="mt-2" :loading="uploading" @click="uploadSelected">
                  {{ uploading ? `上传中 ${uploadProgress}...` : `上传所选 ${uploadFiles.length} 张图片` }}
                </t-button>
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
                <t-button v-if="pasteFiles.length" variant="outline" size="small" class="mt-2" :loading="pasteUploading" @click="uploadPasted">上传并添加图片</t-button>
              </t-tab-panel>
            </t-tabs>
            <div v-if="imgUrls.length" class="flex flex-wrap gap-2 mt-1">
              <div v-for="(url,i) in imgUrls" :key="i" class="relative">
                <img :src="url" class="w-20 h-20 object-cover rounded border" @error="e=>e.target.style.display='none'" />
                <span class="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center text-xs cursor-pointer" @click="removeImg(i)">×</span>
              </div>
            </div>
            <p class="text-xs text-[var(--color-text-tertiary)]">已选 {{ imgUrls.length }} 张，保存后将写入动态图片字段</p>
          </div>
        </t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, Image } from 'lucide-vue-next'
import request from '@/utils/request'

const typeOpts = [
  { label: '建设动态', value: 1 },
  { label: '销售动态', value: 2 },
  { label: '优惠动态', value: 3 },
]
const typeMap = { 1: '建设动态', 2: '销售动态', 3: '优惠动态' }
const typeTheme = { 1: 'primary', 2: 'success', 3: 'warning' }

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterLoupanId = ref(null); const filterType = ref(null)
const pg = reactive({ current: 1, pageSize: 10, total: 0 })

const initForm = () => ({ loupanId: null, title: '', content: '', type: 1, images: '' })
const form = reactive(initForm())

// ===== 图片上传 / 粘贴 =====
const uploadFiles = ref([])
const pasteFiles = ref([])
const pasteUploading = ref(false)
const imgUrls = ref([])

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
    for (const f of pasteFiles.value) {
      let blob = f.blob
      try { blob = await compressImage(blob) } catch {}
      const fd = new FormData(); fd.append('file', blob, 'image.jpg')
      const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
      if (res.url && !imgUrls.value.includes(res.url)) imgUrls.value.push(res.url)
    }
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

const uploading = ref(false)
const uploadProgress = ref('')

async function uploadSelected() {
  if (!uploadFiles.value.length) return
  uploading.value = true
  const total = uploadFiles.value.length
  let ok = 0
  for (let i = 0; i < total; i++) {
    uploadProgress.value = `${i + 1}/${total}`
    try {
      let blob = uploadFiles.value[i].raw
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
      const res = await request.post('/admin/medias/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000,
        onUploadProgress: (e) => {
          if (e.total) uploadProgress.value = `${i + 1}/${total} ${Math.round((e.loaded / e.total) * 100)}%`
        },
      })
      if (res.url && !imgUrls.value.includes(res.url)) imgUrls.value.push(res.url)
      ok++
    } catch (e) {
      console.error('上传失败:', e)
      MessagePlugin.error(`第 ${i + 1} 张上传失败`)
    }
  }
  uploading.value = false
  uploadFiles.value = []
  uploadProgress.value = ''
  MessagePlugin.success(`上传完成：成功 ${ok}/${total} 张`)
}

const cols = [
  { colKey: 'id', title: 'ID', width: 60 },
  { colKey: 'loupanId', title: '楼盘ID', width: 70 },
  { colKey: 'title', title: '动态标题', width: 200, ellipsis: true },
  { colKey: 'type', title: '动态类型', width: 90 },
  { colKey: 'content', title: '动态内容', minWidth: 200, ellipsis: true },
  { colKey: 'images', title: '图片', width: 130 },
  { colKey: 'createTime', title: '创建时间', width: 160 },
  { colKey: 'operation', title: '操作', width: 120, fixed: 'right' },
]

function fmt(t) { if (!t) return ''; const d = new Date(t); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }

async function fetchData() {
  loading.value = true
  try {
    const p = { page: pg.current, size: pg.pageSize }
    if (keyword.value) p.keyword = keyword.value
    if (filterLoupanId.value) p.loupanId = filterLoupanId.value
    if (filterType.value != null) p.type = filterType.value
    const r = await request.get('/admin/dynamics', { params: p })
    data.value = r.records || []; pg.total = r.total || 0
  } catch (e) {} finally { loading.value = false }
}
function search() { pg.current = 1; fetchData() }
function onPg(p) { pg.current = p.current; pg.pageSize = p.pageSize; fetchData() }
function openCreate() {
  isEdit.value = false; editId.value = null
  Object.assign(form, initForm())
  imgUrls.value = []; uploadFiles.value = []; pasteFiles.value = []
  drawer.value = true
}
function openEdit(row) {
  isEdit.value = true; editId.value = row.id
  Object.assign(form, row)
  imgUrls.value = (row.images || '').split(',').filter(Boolean)
  uploadFiles.value = []; pasteFiles.value = []
  drawer.value = true
}
function removeImg(i) { imgUrls.value.splice(i, 1) }
async function save() {
  saving.value = true
  try {
    form.images = imgUrls.value.join(',')
    if (isEdit.value) { await request.put(`/admin/dynamics/${editId.value}`, form); MessagePlugin.success('已更新') }
    else { await request.post('/admin/dynamics', form); MessagePlugin.success('已创建') }
    drawer.value = false; fetchData()
  } catch (e) { MessagePlugin.error(e?.message || '保存失败') } finally { saving.value = false }
}
async function del(id) { await request.delete(`/admin/dynamics/${id}`); MessagePlugin.success('已删除'); fetchData() }

onMounted(fetchData)
</script>
