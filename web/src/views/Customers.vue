<template>
  <div class="customers-page">
    <section class="py-6 bg-[#F8FAFE] min-h-[60vh]">
      <div class="section-container">
        <div class="flex items-center justify-between mb-4">
          <h1 class="text-xl font-bold text-[var(--color-text-primary)]">我的客户</h1>
          <span class="text-xs text-[var(--color-text-tertiary)]">共 {{ total }} 位</span>
        </div>

        <div v-if="loading" class="flex justify-center py-20"><t-loading size="large" text="加载中..." /></div>
        <div v-else-if="!list.length" class="text-center py-20">
          <UsersRound class="w-14 h-14 text-gray-200 mx-auto mb-4" />
          <p class="text-[var(--color-text-tertiary)]">暂无客户，点击右下角"+"录入客户</p>
        </div>
        <div v-else class="space-y-2 md:space-y-3">
          <div v-for="c in list" :key="c.id" class="bg-white rounded-2xl border border-gray-100 p-3 md:p-4 hover:shadow-md transition-all">
            <!-- 三栏：头像 / 信息 / 操作按钮 -->
            <div class="flex items-center gap-2.5 md:gap-3">
              <!-- 左栏：头像（点击编辑） -->
              <div class="w-10 h-10 md:w-12 md:h-12 rounded-full flex-shrink-0 cursor-pointer overflow-hidden" @click="openEdit(c)" title="点击编辑客户">
                <img v-if="c.photo" :src="c.photo" class="w-full h-full object-cover" @error="e=>e.target.style.display='none'" />
                <div v-else class="w-full h-full bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center">
                  <UserIcon class="w-5 h-5 md:w-6 md:h-6 text-[var(--color-primary)]" />
                </div>
              </div>
              <!-- 中栏：信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-1.5">
                  <span v-if="c.intention" :class="['flex-shrink-0 px-1.5 py-0.5 rounded text-xs font-medium',
                    c.intention==='高'?'bg-red-50 text-red-600':c.intention==='中'?'bg-orange-50 text-orange-600':'bg-gray-50 text-gray-500']">{{ c.intention }}</span>
                  <p class="font-bold text-[var(--color-text-primary)] truncate">{{ c.name }}</p>
                  <span v-if="c.phone" class="text-xs text-[var(--color-text-tertiary)] flex items-center gap-0.5 flex-shrink-0">
                    <Phone class="w-3 h-3" />{{ c.phone }}
                  </span>
                  <span v-if="c.shareDesc" class="text-xs text-purple-500 flex items-center gap-0.5 flex-shrink-0" title="分享关系">
                    <Share2 class="w-3 h-3" />{{ c.shareDesc }}
                  </span>
                </div>
                <p v-if="c.remark" class="text-xs text-[var(--color-text-secondary)] mt-1 truncate">
                  <span class="text-[var(--color-text-tertiary)] flex-shrink-0">需求：</span>{{ c.remark }}
                </p>
                <p v-if="c.lastFollowUpTime" class="text-xs text-[var(--color-text-tertiary)] mt-0.5 truncate">
                  最后跟进 {{ fmtDate(c.lastFollowUpTime) }}{{ c.lastFollowUpContent ? '：' + c.lastFollowUpContent : '' }}
                </p>
              </div>
              <!-- 右栏：操作按钮（移动端两行显示，PC 一行） -->
              <div class="grid grid-cols-2 gap-1.5 md:gap-2 flex-shrink-0 md:flex md:flex-row">
                <a v-if="c.phone" :href="`tel:${c.phone}`" title="拨打电话"
                   class="w-8 h-8 md:w-9 md:h-9 rounded-full border-0 outline-none bg-blue-50 text-[var(--color-primary)] flex items-center justify-center hover:bg-blue-100 transition-colors">
                  <Phone class="w-3.5 h-3.5 md:w-4 md:h-4" />
                </a>
                <button title="跟进记录" @click="openFollowUps(c)"
                        class="w-8 h-8 md:w-9 md:h-9 rounded-full border-0 outline-none bg-green-50 text-green-600 flex items-center justify-center hover:bg-green-100 transition-colors">
                  <MessageSquare class="w-3.5 h-3.5 md:w-4 md:h-4" />
                </button>
                <button title="分享" @click="openShare(c)"
                        class="w-8 h-8 md:w-9 md:h-9 rounded-full border-0 outline-none bg-purple-50 text-purple-600 flex items-center justify-center hover:bg-purple-100 transition-colors">
                  <Share2 class="w-3.5 h-3.5 md:w-4 md:h-4" />
                </button>
                <button title="AI分析" @click="openAiSuggest(c)"
                        class="w-8 h-8 md:w-9 md:h-9 rounded-full border-0 outline-none bg-orange-50 text-orange-600 flex items-center justify-center hover:bg-orange-100 transition-colors">
                  <Sparkles class="w-3.5 h-3.5 md:w-4 md:h-4" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="total > pg.pageSize" class="flex justify-center mt-8">
          <t-pagination v-model:current="pg.current" :total="total" :page-size="pg.pageSize" size="small" @change="fetchData" />
        </div>
      </div>
    </section>

    <!-- 右下角操作按钮组 -->
    <div class="fixed right-6 bottom-16 z-30 flex flex-col items-end gap-2">
      <button
        @click="openExcelImport"
        class="flex items-center gap-1.5 px-3.5 md:px-4 py-3 rounded-full border-0 outline-none bg-orange-500 text-white text-sm font-medium shadow-lg hover:bg-orange-600 hover:shadow-xl transition-all"
      >
        <FileSpreadsheet class="w-4 h-4" /><span class="hidden md:inline">批量添加</span>
      </button>
      <button
        @click="openCreate"
        class="flex items-center gap-1.5 px-3.5 md:px-4 py-3 rounded-full border-0 outline-none bg-[var(--color-primary)] text-white text-sm font-medium shadow-lg hover:bg-[var(--color-primary-light)] hover:shadow-xl transition-all"
      >
        <Plus class="w-4 h-4" /><span class="hidden md:inline">添加客户</span>
      </button>
    </div>

    <!-- 录入客户弹窗（手动录入 / AI识别录入） -->
    <t-dialog v-model:visible="createVisible" header="录入客户" width="520px" :confirm-btn="{ content: '保存', loading: saving }" :cancel-btn="{}" @confirm="saveCustomer">
      <t-tabs v-model="createTab" size="medium">
        <t-tab-panel value="manual" label="手动录入">
          <t-form :data="form" label-align="top">
            <t-form-item label="客户姓名"><t-input v-model="form.name" placeholder="客户姓名" /></t-form-item>
            <t-form-item label="手机号"><t-input v-model="form.phone" placeholder="手机号" /></t-form-item>
            <t-form-item label="意向">
              <t-select v-model="form.intention" placeholder="选择意向" clearable :options="[{label:'高',value:'高'},{label:'中',value:'中'},{label:'低',value:'低'}]" />
            </t-form-item>
            <t-form-item label="备注"><t-textarea v-model="form.remark" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" /></t-form-item>
          </t-form>
        </t-tab-panel>
        <t-tab-panel value="ai" label="AI识别录入">
          <t-alert theme="info" message="上传客户资料图片（如名片、登记表、聊天截图等），AI 自动识别姓名、手机号、意向和需求并填入表单。" />
          <div class="mt-3">
            <t-upload v-model="aiFiles" :request-method="aiUploadMethod" :max="5" multiple accept="image/*" theme="image" :auto-upload="false" tips="支持 JPG/PNG/WebP，最多 5 张" />
          </div>
          <div class="flex justify-end mt-3">
            <t-button theme="warning" :loading="aiParsing" :disabled="!aiFiles.length" @click="startAiParse">
              <Sparkles class="w-4 h-4 mr-1" />{{ aiParsing ? '识别中...' : '开始识别' }}
            </t-button>
          </div>
          <div v-if="aiParsing" class="text-center py-6 text-xs text-[var(--color-text-tertiary)]">
            <t-loading size="small" />正在识别图片内容，请稍候...
          </div>
          <div v-else-if="aiParsed" class="mt-3 rounded-lg bg-green-50 border border-green-100 p-3 text-xs text-green-700">
            识别完成，已自动填入表单，请切回"手动录入"核对修改后点击"保存"。
          </div>
        </t-tab-panel>
      </t-tabs>
    </t-dialog>

    <!-- 编辑客户弹窗（点击头像进入，支持修改头像） -->
    <t-dialog v-model:visible="editVisible" header="编辑客户" width="520px" :confirm-btn="{ content: '保存', loading: editSaving }" :cancel-btn="{}" @confirm="saveEditCustomer">
      <div class="flex flex-col items-center mb-4">
        <div class="relative">
          <img v-if="editForm.photo" :src="editForm.photo" class="w-20 h-20 rounded-full object-cover border border-gray-100" @error="e=>e.target.style.display='none'" />
          <div v-else class="w-20 h-20 rounded-full bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center">
            <UserIcon class="w-8 h-8 text-[var(--color-primary)]" />
          </div>
          <label class="absolute -bottom-1 -right-1 w-7 h-7 rounded-full bg-[var(--color-primary)] text-white flex items-center justify-center cursor-pointer shadow">
            <Camera class="w-3.5 h-3.5" />
            <input type="file" accept="image/*" class="hidden" @change="onAvatarUpload" />
          </label>
        </div>
        <p class="text-xs text-[var(--color-text-tertiary)] mt-2">点击相机图标上传/更换头像</p>
      </div>
      <t-form :data="editForm" label-align="top">
        <t-form-item label="客户姓名"><t-input v-model="editForm.name" placeholder="客户姓名" /></t-form-item>
        <t-form-item label="手机号"><t-input v-model="editForm.phone" placeholder="手机号" /></t-form-item>
        <t-form-item label="意向">
          <t-select v-model="editForm.intention" placeholder="选择意向" clearable :options="[{label:'高',value:'高'},{label:'中',value:'中'},{label:'低',value:'低'}]" />
        </t-form-item>
        <t-form-item label="备注"><t-textarea v-model="editForm.remark" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="备注信息" /></t-form-item>
      </t-form>
    </t-dialog>

    <!-- Excel 批量导入客户弹窗 -->
    <t-dialog v-model:visible="excelVisible" header="Excel 批量导入客户" width="520px" :footer="false" :close-on-overlay-click="false">
      <t-alert theme="info" message="上传 Excel 文件（.xlsx/.xls），第一行为表头，需包含『客户姓名』『手机号』列；『意向』『备注』列为选填。" />
      <div class="mt-3">
        <label class="flex flex-col items-center justify-center border-2 border-dashed border-gray-200 rounded-lg py-8 cursor-pointer hover:border-[var(--color-primary)] transition-colors">
          <input type="file" accept=".xlsx,.xls" class="hidden" @change="onExcelFileChange" />
          <FileSpreadsheet class="w-10 h-10 text-gray-300 mb-2" />
          <p class="text-sm text-[var(--color-text-tertiary)]">{{ excelFile ? excelFile.name : '点击选择 Excel 文件' }}</p>
          <p class="text-xs text-[var(--color-text-tertiary)] mt-1">表头示例：客户姓名 | 手机号 | 意向 | 备注</p>
        </label>
      </div>
      <div class="flex justify-end gap-2 mt-4">
        <t-button variant="outline" @click="closeExcelImport">取消</t-button>
        <t-button theme="primary" :loading="excelImporting" :disabled="!excelFile" @click="startExcelImport">开始导入</t-button>
      </div>
      <div v-if="excelResult" class="mt-4 rounded-lg border border-gray-100 bg-gray-50 p-3">
        <div class="flex items-center gap-4 text-sm">
          <span>共 <b class="text-[var(--color-text-primary)]">{{ excelResult.total }}</b> 条</span>
          <span class="text-green-600">成功 <b>{{ excelResult.success }}</b></span>
          <span class="text-red-500">失败 <b>{{ excelResult.failed }}</b></span>
        </div>
        <div v-if="excelResult.errors?.length" class="mt-2 max-h-40 overflow-y-auto">
          <div v-for="(e,i) in excelResult.errors" :key="i" class="text-xs text-red-500 py-0.5">第 {{ e.row }} 行：{{ e.msg }}</div>
        </div>
      </div>
    </t-dialog>

    <!-- 跟进记录抽屉（移动端近全屏，桌面端固定宽度） -->
    <t-drawer v-model:visible="followVisible" :header="`跟进记录 - ${current?.name || ''}`" :size="isMobile ? '100%' : '420px'" :footer="false">
      <div class="mb-3">
        <t-input v-model="followContent" placeholder="填写跟进内容..." class="w-full mb-2" />
        <div class="flex items-center gap-2">
          <t-select v-model="followMethod" placeholder="方式" class="w-24 shrink-0" :options="[{label:'电话',value:'电话'},{label:'微信',value:'微信'},{label:'到访',value:'到访'},{label:'其他',value:'其他'}]" />
          <label class="flex items-center gap-1 px-3 py-2 text-xs rounded-lg bg-blue-50 text-[var(--color-primary)] cursor-pointer hover:bg-blue-100 transition-colors whitespace-nowrap">
            <ImageIcon class="w-3.5 h-3.5" />添加图片
            <input type="file" accept="image/*" class="hidden" @change="onFollowImageUpload" />
          </label>
          <t-button theme="primary" :loading="followSaving" class="flex-1" @click="addFollowUp">添加</t-button>
        </div>
      </div>
      <!-- 跟进图片上传提示 -->
      <div v-if="followUploadUrls.length" class="flex items-center gap-2 mb-4">
        <span class="text-xs text-[var(--color-text-tertiary)]">已选 {{ followUploadUrls.length }} 张</span>
      </div>
      <div v-if="followUploadUrls.length" class="flex flex-wrap gap-2 mb-4">
        <div v-for="(url,i) in followUploadUrls" :key="i" class="relative">
          <img :src="url" class="w-16 h-16 object-cover rounded-lg border border-gray-100" @click="previewImages(followUploadUrls, i)" />
          <span class="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center text-xs cursor-pointer" @click="followUploadUrls.splice(i,1)">×</span>
        </div>
      </div>
      <div v-if="followLoading" class="text-center py-10"><t-loading size="small" /></div>
      <div v-else-if="!follows.length" class="text-center py-10 text-sm text-[var(--color-text-tertiary)]">暂无跟进记录</div>
      <div v-else class="space-y-3">
        <div v-for="f in follows" :key="f.id" class="border border-gray-100 rounded-xl p-3">
          <div class="flex items-center gap-2 mb-1">
            <span v-if="f.method" class="px-1.5 py-0.5 text-xs rounded bg-blue-50 text-[var(--color-primary)]">{{ f.method }}</span>
            <span class="text-xs font-medium text-[var(--color-text-primary)]">{{ f.userNickname || '我' }}</span>
            <span class="text-xs text-[var(--color-text-tertiary)] ml-auto">{{ fmtTime(f.followUpTime || f.createdAt) }}</span>
          </div>
          <p class="text-sm text-[var(--color-text-secondary)]">{{ f.content }}</p>
          <div v-if="parsePhotos(f.photos).length" class="flex flex-wrap gap-2 mt-2">
            <img v-for="(url,i) in parsePhotos(f.photos)" :key="i" :src="url" class="w-20 h-20 object-cover rounded-lg border border-gray-100 cursor-pointer hover:opacity-90 transition-opacity" @error="e=>e.target.style.display='none'" @click="previewImages(parsePhotos(f.photos), i)" />
          </div>
        </div>
      </div>
      <div class="mt-4">
        <t-button block variant="outline" @click="followVisible = false">关闭</t-button>
      </div>
    </t-drawer>

    <!-- 图片全屏预览 -->
    <t-image-viewer v-model:visible="viewerVisible" :images="viewerImages" :default-index="viewerIndex" />

    <!-- 分享弹窗 -->
    <t-dialog v-model:visible="shareVisible" header="分享客户" width="380px" :confirm-btn="{ content: '分享', loading: sharing }" :cancel-btn="{}" @confirm="doShare">
      <p class="text-sm text-[var(--color-text-secondary)] mb-3">分享"{{ current?.name }}"给其他用户：</p>
      <t-select
        v-model="shareTargetId"
        filterable
        clearable
        placeholder="输入名字或手机号搜索用户"
        :options="shareUserOpts"
        :loading="searching"
        style="width:100%"
        @search="searchShareUsers"
      />
      <p class="text-xs text-[var(--color-text-tertiary)] mt-2">输入对方昵称或手机号，从搜索结果中选择</p>
    </t-dialog>

    <!-- AI 沟通文案弹窗 -->
    <t-dialog v-model:visible="aiVisible" header="AI 沟通文案" width="420px" :footer="false" :close-on-overlay-click="false">
      <div class="min-h-[120px]">
        <div v-if="aiLoading" class="flex flex-col items-center justify-center py-10 gap-2">
          <t-loading size="small" />
          <p class="text-xs text-[var(--color-text-tertiary)]">正在AI分析客户信息，请稍候...</p>
        </div>
        <div v-else-if="aiCopy" class="space-y-3">
          <div class="bg-orange-50 border border-orange-100 rounded-xl p-4 text-sm leading-6 text-[var(--color-text-secondary)] whitespace-pre-wrap">{{ aiCopy }}</div>
          <div class="flex justify-end gap-2">
            <t-button variant="outline" @click="aiVisible=false">关闭</t-button>
            <t-button theme="primary" @click="copyAiCopy">复制文案</t-button>
          </div>
        </div>
        <div v-else class="flex items-center justify-center py-10">
          <p class="text-xs text-[var(--color-text-tertiary)]">AI分析失败，请重试</p>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Phone, UsersRound, User as UserIcon, MessageSquare, Share2, Image as ImageIcon, Sparkles, FileSpreadsheet, Camera } from 'lucide-vue-next'
import request from '@/utils/request'

const router = useRouter()
const isMobile = ref(window.innerWidth < 768)
function onResize() { isMobile.value = window.innerWidth < 768 }
const list = ref([])
const loading = ref(false)
const total = ref(0)
const pg = reactive({ current: 1, pageSize: 10 })

const form = reactive({ name: '', phone: '', intention: '', remark: '' })
const createVisible = ref(false)
const saving = ref(false)

// ===== AI 识别录入 =====
const createTab = ref('manual')
const aiFiles = ref([])
const aiParsing = ref(false)
const aiParsed = ref(false)
function aiUploadMethod() {}
async function startAiParse() {
  if (!aiFiles.value.length) { MessagePlugin.warning('请先上传客户资料图片'); return }
  const fd = new FormData()
  aiFiles.value.forEach(f => fd.append('files', f.raw))
  aiParsing.value = true
  aiParsed.value = false
  try {
    const res = await request.post('/user/customers/ai-parse', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
    const fields = res?.fields || {}
    if (!fields.name && !fields.phone) {
      MessagePlugin.warning('AI 未能识别出有效信息，请检查图片清晰度或更换图片')
      return
    }
    Object.assign(form, {
      name: fields.name || form.name || '',
      phone: fields.phone || form.phone || '',
      intention: ['高', '中', '低'].includes(fields.intention) ? fields.intention : form.intention,
      remark: fields.remark || form.remark || ''
    })
    aiParsed.value = true
    MessagePlugin.success('识别成功，已填入表单')
  } catch (e) {
    MessagePlugin.error(e?.response?.data?.msg || e?.message || 'AI识别失败')
  } finally { aiParsing.value = false }
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
    const res = await request.post('/user/customers/excel-import', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
    excelResult.value = res
    MessagePlugin.success(`导入完成：成功 ${res?.success || 0} 条，失败 ${res?.failed || 0} 条`)
    fetchData()
  } catch (e) {
    MessagePlugin.error(e?.response?.data?.msg || e?.message || '导入失败')
  } finally { excelImporting.value = false }
}

const current = ref(null)
const followVisible = ref(false)
const follows = ref([])
const followLoading = ref(false)
const followSaving = ref(false)
const followContent = ref('')
const followMethod = ref('电话')
const followUploadUrls = ref([])
const followUploading = ref(false)

// 图片全屏预览
const viewerVisible = ref(false)
const viewerImages = ref([])
const viewerIndex = ref(0)
function previewImages(images, index) {
  viewerImages.value = images || []
  viewerIndex.value = index || 0
  viewerVisible.value = true
}
function parsePhotos(photos) {
  if (!photos) return []
  try {
    const arr = typeof photos === 'string' ? JSON.parse(photos) : photos
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}
async function onFollowImageUpload(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  followUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const url = await request.post('/user/upload', fd)
    if (url && !followUploadUrls.value.includes(url)) followUploadUrls.value.push(url)
  } catch (e2) { MessagePlugin.error(e2?.message || '上传失败') } finally { followUploading.value = false }
}

const shareVisible = ref(false)
const shareTargetId = ref(null)
const shareUserOpts = ref([])
const searching = ref(false)
const sharing = ref(false)

const aiVisible = ref(false)
const aiLoading = ref(false)
const aiCopy = ref('')

async function fetchData() {
  loading.value = true
  try {
    const r = await request.get('/user/customers', { params: { page: pg.current, size: pg.pageSize } })
    list.value = r?.records || []
    total.value = r?.total || 0
  } catch { router.push('/login') } finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { name: '', phone: '', intention: '', remark: '' })
  createTab.value = 'manual'
  aiFiles.value = []
  aiParsed.value = false
  createVisible.value = true
}
async function saveCustomer() {
  if (!form.name.trim()) { MessagePlugin.warning('请填写客户姓名'); return }
  saving.value = true
  try {
    await request.post('/user/customers', form)
    MessagePlugin.success('录入成功')
    createVisible.value = false
    fetchData()
  } catch (e) { MessagePlugin.error(e?.message || '录入失败') } finally { saving.value = false }
}

// ===== 编辑客户（点击头像进入） =====
const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({ id: null, name: '', phone: '', intention: '', remark: '', photo: '' })
function openEdit(c) {
  Object.assign(editForm, { id: c.id, name: c.name || '', phone: c.phone || '', intention: c.intention || '', remark: c.remark || '', photo: c.photo || '' })
  editVisible.value = true
}
async function onAvatarUpload(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) { MessagePlugin.warning('请选择图片文件'); return }
  if (file.size > 5 * 1024 * 1024) { MessagePlugin.warning('图片不能超过5MB'); return }
  try {
    const fd = new FormData()
    fd.append('file', file)
    const url = await request.post('/user/upload', fd)
    if (url) editForm.photo = url
    MessagePlugin.success('头像已更新，点击保存生效')
  } catch (e2) { MessagePlugin.error(e2?.message || '头像上传失败') }
}
async function saveEditCustomer() {
  if (!editForm.name.trim()) { MessagePlugin.warning('请填写客户姓名'); return }
  editSaving.value = true
  try {
    await request.put(`/user/customers/${editForm.id}`, {
      name: editForm.name,
      phone: editForm.phone,
      intention: editForm.intention,
      remark: editForm.remark,
      photo: editForm.photo
    })
    MessagePlugin.success('保存成功')
    editVisible.value = false
    fetchData()
  } catch (e) { MessagePlugin.error(e?.message || '保存失败') } finally { editSaving.value = false }
}

async function openFollowUps(c) {
  current.value = c
  followVisible.value = true
  followContent.value = ''
  followUploadUrls.value = []
  followLoading.value = true
  try {
    follows.value = await request.get(`/user/customers/${c.id}/follow-ups`) || []
  } catch { follows.value = [] } finally { followLoading.value = false }
}
async function addFollowUp() {
  if (!followContent.value.trim()) { MessagePlugin.warning('请填写跟进内容'); return }
  followSaving.value = true
  try {
    await request.post(`/user/customers/${current.value.id}/follow-ups`, {
      content: followContent.value,
      method: followMethod.value,
      photos: followUploadUrls.value.length ? JSON.stringify(followUploadUrls.value) : null
    })
    MessagePlugin.success('已添加')
    followContent.value = ''
    followUploadUrls.value = []
    follows.value = await request.get(`/user/customers/${current.value.id}/follow-ups`) || []
  } catch (e) { MessagePlugin.error(e?.message || '添加失败') } finally { followSaving.value = false }
}

function openShare(c) {
  current.value = c
  shareTargetId.value = null
  shareUserOpts.value = []
  shareVisible.value = true
}
async function searchShareUsers(keyword) {
  if (!keyword || !keyword.trim()) return
  searching.value = true
  try {
    const list = await request.get('/user/customers/user-search', { params: { keyword } }) || []
    shareUserOpts.value = list.map(u => ({ label: `${u.nickname || '未命名'}${u.phone ? '（'+u.phone+'）' : ''}`, value: u.id }))
  } catch { shareUserOpts.value = [] } finally { searching.value = false }
}
async function doShare() {
  if (!shareTargetId.value) { MessagePlugin.warning('请选择要分享的用户'); return }
  sharing.value = true
  try {
    await request.post(`/user/customers/${current.value.id}/share`, { userId: shareTargetId.value })
    MessagePlugin.success('分享成功')
    shareVisible.value = false
  } catch (e) { MessagePlugin.error(e?.response?.data?.msg || e?.message || '分享失败') } finally { sharing.value = false }
}

async function openAiSuggest(c) {
  current.value = c
  aiCopy.value = ''
  aiVisible.value = true
  aiLoading.value = true
  try {
    aiCopy.value = await request.post(`/user/customers/${c.id}/ai-suggest`) || ''
  } catch (e) {
    aiCopy.value = ''
    MessagePlugin.error(e?.response?.data?.msg || e?.message || 'AI分析失败')
  } finally { aiLoading.value = false }
}
async function copyAiCopy() {
  try {
    await navigator.clipboard.writeText(aiCopy.value)
    MessagePlugin.success('已复制')
  } catch { MessagePlugin.error('复制失败') }
}

function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
function fmtDate(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', onResize)
})
onUnmounted(() => window.removeEventListener('resize', onResize))
</script>
