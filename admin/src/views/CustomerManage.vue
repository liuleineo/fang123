<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">客户管理</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理客户信息及所属用户</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建客户</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50 flex-wrap">
        <t-input v-model="keyword" placeholder="搜索客户姓名/手机号" clearable class="w-[220px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-select v-model="filterUserId" placeholder="按所属用户筛选" clearable filterable class="w-[200px]" :options="userOpts" @change="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterUserId='';search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #photo="{ row }">
          <t-image v-if="row.photo" :src="row.photo" fit="cover" class="w-10 h-10 rounded-full" />
          <div v-else class="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center text-gray-300"><UserIcon class="w-5 h-5" /></div>
        </template>
        <template #intention="{ row }">
          <t-tag v-if="row.intention" size="small" :theme="row.intention==='高'?'danger':row.intention==='中'?'warning':'default'">{{ row.intention }}</t-tag>
          <span v-else class="text-xs text-gray-300">-</span>
        </template>
        <template #userId="{ row }">
          <span v-if="row.userId" class="flex flex-col leading-tight">
            <span class="font-medium text-[var(--color-primary)]">{{ row.userId }}</span>
            <span class="text-xs text-[var(--color-text-tertiary)] truncate max-w-[120px]">{{ row.userNickname || '未知用户' }}</span>
          </span>
          <span v-else class="text-xs text-gray-300">-</span>
        </template>
        <template #sharedTo="{ row }">
          <span v-if="row.sharedTo" class="text-xs text-[var(--color-text-secondary)]">{{ row.sharedTo }}</span>
          <span v-else class="text-xs text-gray-300">-</span>
        </template>
        <template #lastFollowUpTime="{ row }">
          <span v-if="row.lastFollowUpTime" class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.lastFollowUpTime) }}</span>
          <span v-else class="text-xs text-gray-300">-</span>
        </template>
        <template #createdAt="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createdAt) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openFollows(row)">查看跟进</t-button>
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑客户':'新建客户'" size="480px" :footer="false">
      <t-form :data="form" label-align="top">
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="客户姓名"><t-input v-model="form.name" :maxlength="100" /></t-form-item>
          <t-form-item label="手机号"><t-input v-model="form.phone" :maxlength="20" /></t-form-item>
        </div>
        <t-form-item label="所属用户" help="选择该客户归属的C端用户（关联 user_info 表）">
          <t-select v-model="form.userId" filterable clearable placeholder="搜索昵称/手机号选择用户" :options="userOpts" class="w-full" />
        </t-form-item>
        <t-form-item label="意向">
          <t-select v-model="form.intention" clearable placeholder="请选择" :options="[{label:'高',value:'高'},{label:'中',value:'中'},{label:'低',value:'低'}]" class="w-full" />
        </t-form-item>
        <t-form-item label="客户头像URL"><t-input v-model="form.photo" placeholder="https://..." /></t-form-item>
        <t-form-item label="备注"><t-textarea v-model="form.remark" :autosize="{ minRows: 3, maxRows: 6 }" placeholder="客户备注信息" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>

    <!-- 跟进记录弹窗 -->
    <t-dialog v-model:visible="followVisible" :header="`跟进记录 - ${current?.name || ''}`" width="560px" :footer="false">
      <div v-if="followLoading" class="text-center py-10"><t-loading size="small" /></div>
      <div v-else-if="!follows.length" class="text-center py-10 text-sm text-[var(--color-text-tertiary)]">暂无跟进记录</div>
      <div v-else class="space-y-3 max-h-[60vh] overflow-y-auto">
        <div v-for="f in follows" :key="f.id" class="border border-gray-100 rounded-xl p-3">
          <div class="flex items-center gap-2 mb-1">
            <span v-if="f.method" class="px-1.5 py-0.5 text-xs rounded bg-blue-50 text-[var(--color-primary)]">{{ f.method }}</span>
            <span class="text-xs font-medium text-[var(--color-text-primary)]">{{ f.userNickname || '未知' }}</span>
            <span class="text-xs text-[var(--color-text-tertiary)] ml-auto">{{ fmtTime(f.followUpTime || f.createdAt) }}</span>
          </div>
          <p class="text-sm text-[var(--color-text-secondary)] whitespace-pre-wrap">{{ f.content }}</p>
          <div v-if="parsePhotos(f.photos).length" class="flex flex-wrap gap-2 mt-2">
            <img v-for="(url,i) in parsePhotos(f.photos)" :key="i" :src="url" class="w-16 h-16 object-cover rounded-lg border border-gray-100 cursor-pointer hover:opacity-90 transition-opacity" @click="previewImages(parsePhotos(f.photos), i)" />
          </div>
        </div>
      </div>
    </t-dialog>

    <!-- 图片全屏预览 -->
    <t-image-viewer v-model:visible="viewerVisible" :images="viewerImages" :default-index="viewerIndex" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, User as UserIcon } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterUserId = ref(null)
const pg = reactive({ current: 1, pageSize: 10, total: 0 })

// ===== 所属用户选项（复用 /admin/users 接口）=====
const userOpts = ref([])
async function fetchUserOpts(keyword) {
  try {
    const params = { page: 1, size: 50 }
    if (keyword) params.keyword = keyword
    const r = await request.get('/admin/users', { params })
    const list = r?.records || []
    userOpts.value = list.map(u => ({ label: `${u.id} · ${u.nickname || '未命名'}${u.phone ? '（'+u.phone+'）' : ''}`, value: u.id }))
  } catch {}
}

const initForm = () => ({ name: '', phone: '', remark: '', intention: '', userId: null, photo: '' })
const form = reactive(initForm())

const cols = [
  { colKey: 'id', title: 'ID', width: 60 },
  { colKey: 'photo', title: '头像', width: 70 },
  { colKey: 'name', title: '客户姓名', width: 120, ellipsis: true },
  { colKey: 'phone', title: '手机号', width: 130 },
  { colKey: 'userId', title: '所属用户', width: 150 },
  { colKey: 'sharedTo', title: '被分享人', width: 130, ellipsis: true },
  { colKey: 'intention', title: '意向', width: 70 },
  { colKey: 'remark', title: '备注', minWidth: 160, ellipsis: true },
  { colKey: 'lastFollowUpTime', title: '最后跟进时间', width: 160 },
  { colKey: 'createdAt', title: '创建时间', width: 160 },
  { colKey: 'operation', title: '操作', width: 120, fixed: 'right' },
]

function fmt(t) { if (!t) return ''; const d = new Date(t); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}` }

async function fetchData() {
  loading.value = true
  try {
    const p = { page: pg.current, size: pg.pageSize }
    if (keyword.value) p.keyword = keyword.value
    if (filterUserId.value) p.userId = filterUserId.value
    const r = await request.get('/admin/customers', { params: p })
    data.value = r.records || []; pg.total = r.total || 0
  } catch (e) {} finally { loading.value = false }
}
function search() { pg.current = 1; fetchData() }
function onPg(p) { pg.current = p.current; pg.pageSize = p.pageSize; fetchData() }
function openCreate() { isEdit.value = false; editId.value = null; Object.assign(form, initForm()); drawer.value = true }
function openEdit(row) { isEdit.value = true; editId.value = row.id; Object.assign(form, row); drawer.value = true }
async function save() {
  saving.value = true
  try {
    if (isEdit.value) { await request.put(`/admin/customers/${editId.value}`, form); MessagePlugin.success('已更新') }
    else { await request.post('/admin/customers', form); MessagePlugin.success('已创建') }
    drawer.value = false; fetchData()
  } catch (e) { MessagePlugin.error(e?.message || '保存失败') } finally { saving.value = false }
}
async function del(id) { await request.delete(`/admin/customers/${id}`); MessagePlugin.success('已删除'); fetchData() }

// ===== 跟进记录 =====
const current = ref(null)
const followVisible = ref(false)
const follows = ref([])
const followLoading = ref(false)
async function openFollows(row) {
  current.value = row
  followVisible.value = true
  followLoading.value = true
  try {
    follows.value = await request.get(`/admin/customers/${row.id}/follow-ups`) || []
  } catch { follows.value = [] } finally { followLoading.value = false }
}
function parsePhotos(photos) {
  if (!photos) return []
  try { const arr = typeof photos === 'string' ? JSON.parse(photos) : photos; return Array.isArray(arr) ? arr : [] } catch { return [] }
}
const viewerVisible = ref(false); const viewerImages = ref([]); const viewerIndex = ref(0)
function previewImages(images, index) {
  viewerImages.value = images || []
  viewerIndex.value = index || 0
  viewerVisible.value = true
}
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

onMounted(() => { fetchData(); fetchUserOpts() })
</script>
