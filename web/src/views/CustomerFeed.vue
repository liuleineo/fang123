<template>
  <div class="feed-page">
    <!-- 顶部悬浮栏 -->
    <div class="feed-top">
      <button class="feed-icon-btn" aria-label="返回" @click="goBack">
        <ChevronLeft class="w-5 h-5" />
      </button>
      <div class="text-xs text-[var(--color-text-secondary)]">
        <span class="text-sm font-semibold text-[var(--color-text-primary)]">{{ list.length ? currentIndex + 1 : 0 }}</span>
        <span class="mx-0.5">/</span>{{ total || list.length }}
      </div>
      <div class="w-8"></div>
    </div>

    <!-- 加载 / 空态 -->
    <div v-if="!list.length && loading" class="feed-center">
      <t-loading size="large" text="正在加载客户..." />
    </div>
    <div v-else-if="!list.length" class="feed-center">
      <UsersRound class="w-12 h-12 text-white/15 mb-3" />
      <p class="text-sm text-[var(--color-text-tertiary)]">还没有可跟进的客户</p>
      <t-button class="mt-4" theme="primary" @click="goBack">返回</t-button>
    </div>

    <!-- 垂直信息流：每屏一个客户，上下滑动切换 -->
    <div v-else ref="scrollerRef" class="feed-scroller" tabindex="0" @scroll.passive="onScroll">
      <section v-for="(c, i) in list" :key="c.id" class="feed-slide">
        <!-- 客户资料 -->
        <div class="feed-card">
          <div class="flex items-start gap-3">
            <div class="relative w-14 h-14 rounded-[6px] overflow-hidden flex-shrink-0 bg-[#1d2330] flex items-center justify-center cursor-pointer"
              title="点击修改客户资料" @click.stop="openEdit(c)">
              <img v-if="c.photo" :key="c.photo" :src="c.photo" class="w-full h-full object-cover" @error="e => (e.target.style.display = 'none')" />
              <UserIcon v-else class="w-7 h-7 text-[var(--color-primary)]" />
              <span class="absolute bottom-0 right-0 w-4 h-4 bg-black/60 flex items-center justify-center rounded-tl-[4px]">
                <Pencil class="w-2.5 h-2.5 text-white/85" />
              </span>
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h2 class="text-lg font-bold text-[var(--color-text-primary)] truncate">{{ c.name || '未命名客户' }}</h2>
                <span v-if="c.intention" :class="['text-[11px] px-1.5 py-0.5 rounded font-medium', intentionClass(c.intention)]">{{ c.intention }}意向</span>
                <span v-if="c.sharedTo" class="text-[11px] px-1.5 py-0.5 rounded bg-white/10 text-[var(--color-text-tertiary)]">共享</span>
              </div>
              <p class="mt-1 text-base font-semibold tracking-wide text-[var(--color-text-primary)]">{{ c.phone || '未填写手机号' }}</p>
              <p class="mt-1 text-xs text-[var(--color-text-tertiary)]">
                <span :class="idleClass(c)">{{ idleText(c) }}</span>
              </p>
            </div>
          </div>

          <div v-if="c.remark" class="mt-3 text-sm leading-6 text-[var(--color-text-secondary)] bg-white/[0.045] rounded-[4px] px-3 py-2 whitespace-pre-wrap max-h-[110px] overflow-y-auto overscroll-y-contain">
            {{ c.remark }}
          </div>
        </div>

        <!-- 跟进记录 -->
        <div class="feed-follows">
          <div class="flex items-center justify-between mb-2">
            <h3 class="text-sm font-semibold text-[var(--color-text-primary)]">跟进记录</h3>
            <span class="text-xs text-[var(--color-text-tertiary)]">
              {{ followState(c.id).loading ? '加载中...' : `共 ${followState(c.id).items.length} 条` }}
            </span>
          </div>

          <div v-if="followState(c.id).loading" class="flex justify-center py-6"><t-loading size="small" /></div>
          <p v-else-if="!followState(c.id).items.length" class="text-xs text-[var(--color-text-tertiary)] py-4 text-center">
            还没有跟进记录，点击下方「增加跟进」记录第一次沟通
          </p>
          <div v-else class="space-y-2">
            <div v-for="f in followState(c.id).items.slice(0, 5)" :key="f.id" class="bg-white/[0.045] rounded-[4px] px-3 py-2">
              <div class="flex items-center gap-2 mb-1">
                <span v-if="f.method" class="text-[11px] px-1.5 py-0.5 rounded bg-white/10 text-[var(--color-primary)] border border-white/10">{{ f.method }}</span>
                <span class="text-[11px] text-[var(--color-text-tertiary)] ml-auto">{{ fmtTime(f.followUpTime || f.createdAt) }}</span>
              </div>
              <p class="text-sm leading-5 text-[var(--color-text-secondary)] whitespace-pre-wrap line-clamp-3">{{ f.content }}</p>
              <!-- 跟进图片：固定尺寸缩略图（最多 6 张，避免刷屏），点击全屏预览 -->
              <div v-if="parsePhotos(f.photos).length" class="follow-photos">
                <img v-for="(url, pi) in parsePhotos(f.photos).slice(0, 6)" :key="pi" :src="url" alt=""
                  class="follow-photo" @error="onPhotoError" @click.stop="previewImages(parsePhotos(f.photos), pi)" />
                <span v-if="parsePhotos(f.photos).length > 6" class="follow-photo-more">+{{ parsePhotos(f.photos).length - 6 }}</span>
              </div>
            </div>
            <p v-if="followState(c.id).items.length > 5" class="text-[11px] text-[var(--color-text-tertiary)] text-center pt-1">
              仅显示最近 5 条，共 {{ followState(c.id).items.length }} 条
            </p>
          </div>
        </div>

        <p class="feed-hint">
          {{ i === 0 ? '上下滑动切换客户' : i === list.length - 1 ? (hasMore ? '继续上滑加载更多客户' : '已经是最后一位客户') : '上下滑动切换客户' }}
        </p>
      </section>

      <section v-if="hasMore" class="feed-slide feed-center">
        <t-loading v-if="loading" size="small" text="加载更多客户..." />
        <p v-else class="text-xs text-[var(--color-text-tertiary)]">继续上滑加载更多</p>
      </section>
    </div>

    <!-- 底部操作栏 -->
    <div v-if="list.length" class="feed-footer">
      <t-button class="flex-1" variant="outline" size="large" @click="callCustomer">
        <template #icon><Phone class="w-4 h-4" /></template>电话客户
      </t-button>
      <t-button class="flex-1" theme="primary" size="large" @click="openFollow">
        <template #icon><MessageSquarePlus class="w-4 h-4" /></template>增加跟进
      </t-button>
    </div>

    <!-- 占位说明：图片预览组件放在 .feed-page 之外（见模板末尾），避免参与 flex 布局 -->

    <!-- 增加跟进弹窗 -->
    <t-dialog v-model:visible="followVisible" class="feed-dark-dialog" header="增加跟进" width="360px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-3">
        <p class="text-sm text-[var(--color-text-secondary)]">
          客户：<span class="font-medium text-[var(--color-text-primary)]">{{ current?.name || '' }}</span>
        </p>
        <!-- 客户意向：可在记录跟进时顺便调整，选中的再点一次取消 -->
        <div class="flex items-center gap-2">
          <span class="text-sm text-[var(--color-text-secondary)] shrink-0">意向</span>
          <div class="flex items-center gap-1.5">
            <button v-for="lv in intentionLevels" :key="lv" type="button"
              :class="['px-3 py-1 rounded-full text-xs font-medium border-0 outline-none transition-colors',
                followIntention === lv ? intentionActiveClass(lv) : 'bg-white/10 text-[var(--color-text-secondary)]']"
              @click="toggleIntention(lv)">{{ lv }}</button>
          </div>
          <span class="text-[11px] text-[var(--color-text-tertiary)] ml-auto">再点可取消</span>
        </div>
        <div class="flex items-center gap-1.5 flex-wrap">
          <button v-for="m in followMethods" :key="m" type="button"
            :class="['px-3 py-1 rounded-full text-xs font-medium border-0 outline-none transition-colors',
              followMethod === m ? 'bg-[var(--color-primary)] text-white' : 'bg-white/10 text-[var(--color-text-secondary)]']"
            @click="followMethod = m">{{ m }}</button>
        </div>
        <t-textarea v-model="followContent" placeholder="记录本次沟通内容，如：客户想了解89㎡户型，预算300万左右" :autosize="{ minRows: 3, maxRows: 6 }" />
        <t-button block theme="primary" :loading="followSaving" @click="submitFollow">保存跟进</t-button>
      </div>
    </t-dialog>

    <!-- 修改客户资料（点击头像打开） -->
    <t-dialog v-model:visible="editVisible" class="feed-dark-dialog" header="修改客户资料" width="360px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-3">
        <!-- 头像：点击预览图选图，或在任意位置 Ctrl+V 粘贴图片 -->
        <div class="flex items-center gap-3">
          <div class="relative w-16 h-16 rounded-[6px] overflow-hidden shrink-0 bg-[#1d2330] border border-[#2a303a] flex items-center justify-center cursor-pointer"
            title="点击更换头像" @click="pickAvatar">
            <img v-if="editForm.photo" :key="editForm.photo" :src="editForm.photo" class="w-full h-full object-cover"
              @error="e => (e.target.style.display = 'none')" />
            <UserIcon v-else class="w-7 h-7 text-[var(--color-primary)]" />
            <span v-if="avatarUploading" class="absolute inset-0 bg-black/60 flex items-center justify-center">
              <t-loading size="small" />
            </span>
            <span v-else class="absolute bottom-0 right-0 w-4 h-4 bg-black/60 flex items-center justify-center rounded-tl-[4px]">
              <Camera class="w-2.5 h-2.5 text-white/85" />
            </span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="avatar-paste-zone" :class="{ 'is-busy': avatarUploading }" @click="pickAvatar">
              <ImageIcon class="w-4 h-4 shrink-0" />
              <span class="text-xs">点击上传，或 Ctrl+V 粘贴</span>
            </div>
            <div class="flex items-center gap-2 mt-1">
              <p class="text-[11px] text-[var(--color-text-tertiary)]">支持 JPG/PNG/GIF/WebP，5MB 内</p>
              <button v-if="editForm.photo" type="button" class="avatar-clear" @click.stop="clearAvatar">清除</button>
            </div>
          </div>
          <input ref="avatarInputRef" type="file" accept="image/*" class="hidden" @change="onAvatarPick" />
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-[var(--color-text-secondary)] w-14 shrink-0">姓名</span>
          <t-input v-model="editForm.name" placeholder="客户姓名" :maxlength="100" />
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-[var(--color-text-secondary)] w-14 shrink-0">手机号</span>
          <t-input v-model="editForm.phone" placeholder="手机号" :maxlength="20" />
        </div>
        <div class="flex items-center gap-2">
          <span class="text-sm text-[var(--color-text-secondary)] w-14 shrink-0">意向</span>
          <div class="flex items-center gap-1.5">
            <button v-for="lv in intentionLevels" :key="lv" type="button"
              :class="['px-3 py-1 rounded-full text-xs font-medium border-0 outline-none transition-colors',
                editForm.intention === lv ? intentionActiveClass(lv) : 'bg-white/10 text-[var(--color-text-secondary)]']"
              @click="toggleEditIntention(lv)">{{ lv }}</button>
          </div>
          <span class="text-[11px] text-[var(--color-text-tertiary)] ml-auto">再点可取消</span>
        </div>
        <div class="flex items-start gap-2">
          <span class="text-sm text-[var(--color-text-secondary)] w-14 shrink-0 pt-2">需求备注</span>
          <t-textarea v-model="editForm.remark" placeholder="客户需求、预算、意向区域等" :autosize="{ minRows: 3, maxRows: 6 }" />
        </div>
        <t-button block theme="primary" :loading="editSaving" @click="saveCustomerInfo">保存</t-button>
      </div>
    </t-dialog>
  </div>

  <!-- 跟进图片预览：独立于 .feed-page 布局，避免组件容器挤压信息流高度 -->
  <t-image-viewer v-if="viewerVisible" :visible="viewerVisible" :images="viewerImages"
    :default-index="viewerIndex" @close="viewerVisible = false" @update:visible="v => (viewerVisible = v)" />
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { ChevronLeft, Phone, MessageSquarePlus, UsersRound, User as UserIcon, Pencil, Camera, Image as ImageIcon } from 'lucide-vue-next'
import request from '@/utils/request'

const router = useRouter()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const currentIndex = ref(0)
const scrollerRef = ref(null)
/** 一次拉全量后本地排序，不存在"还有更多" */
const hasMore = ref(false)
const current = computed(() => list.value[currentIndex.value] || null)

/** 每次请求的页大小 */
const FETCH_SIZE = 500
/** 意向权重：高的排前面，无意向的排最后 */
function intentionWeight(c) {
  if (c?.intention === '高') return 3
  if (c?.intention === '中') return 2
  if (c?.intention === '低') return 1
  return 0
}

/** Fisher-Yates 洗牌（原地） */
function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

/**
 * 客户信息流的展示顺序：意向高的优先，同一意向内部随机打乱。
 * 每次打开页面都会重新洗牌，避免总是同样的顺序、只跟进到前面几个人。
 */
function shuffleByIntention(customers) {
  const groups = new Map()
  customers.forEach(c => {
    const w = intentionWeight(c)
    if (!groups.has(w)) groups.set(w, [])
    groups.get(w).push(c)
  })
  const result = []
  ;[...groups.keys()].sort((a, b) => b - a).forEach(w => {
    result.push(...shuffle(groups.get(w)))
  })
  return result
}

/** 拉全量客户（客户量在几百级别，一次拉完才能做全局随机排序） */
async function fetchAllCustomers() {
  const all = []
  let page = 1
  let totalCount = 0
  while (page <= 10) {
    const r = await request.get('/user/customers', {
      params: { page, size: FETCH_SIZE, sortField: 'lastFollowUpTime', sortOrder: 'asc' }
    })
    const records = r?.records || []
    totalCount = Number(r?.total || 0)
    all.push(...records)
    if (!records.length || all.length >= totalCount) break
    page += 1
  }
  return all
}

async function fetchList() {
  if (loading.value) return
  loading.value = true
  try {
    const all = await fetchAllCustomers()
    list.value = shuffleByIntention(all)
    total.value = list.value.length
    currentIndex.value = 0
    await nextTick()
    scrollerRef.value?.scrollTo({ top: 0 })
    ensureFollows(0)
  } catch (e) {
    MessagePlugin.error(e?.message || '客户加载失败')
  } finally {
    loading.value = false
  }
}

/** 跟进记录缓存：id -> { items, loaded, loading } */
const followCache = reactive({})
function followState(id) {
  return followCache[id] || { items: [], loaded: false, loading: true }
}
async function loadFollows(c) {
  if (!c) return
  const cached = followCache[c.id]
  if (cached && (cached.loaded || cached.loading)) return
  followCache[c.id] = { items: [], loaded: false, loading: true }
  try {
    const r = await request.get(`/user/customers/${c.id}/follow-ups`)
    followCache[c.id] = { items: r || [], loaded: true, loading: false }
  } catch {
    followCache[c.id] = { items: [], loaded: false, loading: false }
  }
}
/** 预加载当前及下一位客户的跟进记录，滑动时不出现空白 */
function ensureFollows(index) {
  loadFollows(list.value[index])
  loadFollows(list.value[index + 1])
}

/** 滚动定位当前屏，接近底部时预取下一页 */
let ticking = false
function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    ticking = false
    const el = scrollerRef.value
    if (!el) return
    const h = el.clientHeight || 1
    const idx = Math.min(Math.round(el.scrollTop / h), list.value.length - 1)
    if (idx !== currentIndex.value && idx >= 0) {
      currentIndex.value = idx
      ensureFollows(idx)
    }
    if (idx >= list.value.length - 3 && hasMore.value) fetchList()
  })
}

function idleDays(c) {
  if (!c?.lastFollowUpTime) return null
  const diff = Date.now() - new Date(c.lastFollowUpTime).getTime()
  return Math.max(0, Math.floor(diff / 86400000))
}
function idleText(c) {
  const d = idleDays(c)
  if (d === null) return '还没有跟进过'
  if (d === 0) return '今天已跟进'
  return `已 ${d} 天未跟进`
}
function idleClass(c) {
  const d = idleDays(c)
  if (d === null || d >= 7) return 'text-red-400 font-medium'
  if (d >= 3) return 'text-orange-400'
  return 'text-[var(--color-text-tertiary)]'
}
function intentionClass(v) {
  // 深色主题下用带透明度的底色，避免浅色块刺眼
  if (v === '高') return 'bg-red-500/15 text-red-400'
  if (v === '中') return 'bg-orange-500/15 text-orange-400'
  return 'bg-white/10 text-[var(--color-text-tertiary)]'
}
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/** 跟进图片字段存的是 JSON 数组字符串，兼容已是数组的情况 */
function parsePhotos(photos) {
  if (!photos) return []
  try {
    const arr = typeof photos === 'string' ? JSON.parse(photos) : photos
    return Array.isArray(arr) ? arr.filter(Boolean) : []
  } catch { return [] }
}

/** 图片加载失败时替换为透明占位，避免出现裂图图标 */
const TRANSPARENT_GIF = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'
function onPhotoError(e) {
  if (e?.target) e.target.src = TRANSPARENT_GIF
}

// ===== 图片全屏预览 =====
const viewerVisible = ref(false)
const viewerImages = ref([])
const viewerIndex = ref(0)
function previewImages(images, index) {
  viewerImages.value = images || []
  viewerIndex.value = index || 0
  viewerVisible.value = true
}

// ===== 电话客户 =====
/** 直接调起拨号（tel: 协议），不做复制号码 */
function callCustomer() {
  const phone = String(current.value?.phone || '').replace(/[\s-]/g, '')
  if (!phone) { MessagePlugin.warning('该客户没有填写手机号'); return }
  window.location.href = `tel:${phone}`
}

// ===== 增加跟进 =====
const followMethods = ['电话', '微信', '到访', '其他']
const followVisible = ref(false)
const followContent = ref('')
const followMethod = ref('电话')
const followSaving = ref(false)

// ===== 客户意向（可在记录跟进时顺手调整）=====
const intentionLevels = ['高', '中', '低']
const followIntention = ref('')
function toggleIntention(level) {
  // 选中同一个再点一次 = 取消意向
  followIntention.value = followIntention.value === level ? '' : level
}
function intentionActiveClass(level) {
  if (level === '高') return 'bg-red-500/85 text-white'
  if (level === '中') return 'bg-orange-500/85 text-white'
  return 'bg-slate-500/80 text-white'
}

function openFollow() {
  if (!current.value) return
  followContent.value = ''
  followMethod.value = '电话'
  followIntention.value = current.value?.intention || ''
  followVisible.value = true
}

async function submitFollow() {
  const content = followContent.value.trim()
  if (!content) { MessagePlugin.warning('请填写跟进内容'); return }
  followSaving.value = true
  const c = current.value
  try {
    const created = await request.post(`/user/customers/${c.id}/follow-ups`, {
      content,
      method: followMethod.value
    })
    // 同步当前客户卡片上的"最后跟进"，并刷新该客户记录
    c.lastFollowUpTime = created?.followUpTime || created?.createdAt || new Date().toISOString()
    followCache[c.id] = { items: [], loaded: false, loading: false }
    await loadFollows(c)
    followVisible.value = false
    followContent.value = ''

    // 意向有变化时一并更新客户（归属人才能改，被共享的客户更新会失败，不影响跟进已保存）
    const newIntention = followIntention.value || ''
    if (newIntention !== (c.intention || '')) {
      try {
        await request.put(`/user/customers/${c.id}`, { intention: newIntention })
        c.intention = newIntention
        MessagePlugin.success(newIntention ? `已添加跟进，意向更新为「${newIntention}」` : '已添加跟进，已清除意向')
      } catch {
        MessagePlugin.warning('跟进已保存，但意向修改失败（仅客户归属人可修改意向）')
      }
    } else {
      MessagePlugin.success('已添加跟进')
    }
  } catch (e) {
    MessagePlugin.error(e?.message || '添加失败')
  } finally {
    followSaving.value = false
  }
}

// ===== 修改客户资料（点击头像）=====
const editVisible = ref(false)
const editSaving = ref(false)
const editTarget = ref(null)
const editForm = reactive({ name: '', phone: '', intention: '', remark: '', photo: '' })

function openEdit(c) {
  if (!c) return
  editTarget.value = c
  Object.assign(editForm, {
    name: c.name || '',
    phone: c.phone || '',
    intention: c.intention || '',
    remark: c.remark || '',
    photo: c.photo || ''
  })
  editVisible.value = true
}

// ===== 修改头像（点击上传 + Ctrl+V 粘贴上传）=====
const AVATAR_MAX_SIZE = 5 * 1024 * 1024
const avatarInputRef = ref(null)
const avatarUploading = ref(false)

function pickAvatar() {
  if (avatarUploading.value) return
  avatarInputRef.value?.click()
}

function onAvatarPick(e) {
  const file = e.target?.files?.[0]
  // 清空 input，保证再次选择同一张图片也能触发 change
  if (e.target) e.target.value = ''
  if (file) uploadAvatarFile(file)
}

/** 文档级粘贴监听：仅在资料弹窗打开时接管剪贴板里的图片，文本粘贴不受影响 */
function onDocumentPaste(e) {
  if (!editVisible.value || avatarUploading.value) return
  const items = Array.from(e.clipboardData?.items || [])
  const item = items.find(it => it.kind === 'file' && String(it.type).startsWith('image/'))
  if (!item) return
  const file = item.getAsFile()
  if (!file) return
  e.preventDefault()
  uploadAvatarFile(file)
}

async function uploadAvatarFile(file) {
  if (!file.type?.startsWith('image/')) { MessagePlugin.warning('请选择图片文件'); return }
  if (file.size > AVATAR_MAX_SIZE) { MessagePlugin.warning('图片大小不能超过 5MB'); return }
  avatarUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const url = await request.post('/user/upload/avatar', fd)
    if (!url) throw new Error('上传失败')
    editForm.photo = url
    MessagePlugin.success('头像已更新，点击保存生效')
  } catch (err) {
    MessagePlugin.error(err?.response?.data?.msg || err?.message || '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
}

function clearAvatar() {
  if (avatarUploading.value) return
  editForm.photo = ''
}

function toggleEditIntention(level) {
  editForm.intention = editForm.intention === level ? '' : level
}

async function saveCustomerInfo() {
  const name = editForm.name.trim()
  if (!name) { MessagePlugin.warning('请填写客户姓名'); return }
  const c = editTarget.value
  if (!c) return
  editSaving.value = true
  try {
    const payload = {
      name,
      phone: editForm.phone.trim(),
      intention: editForm.intention || '',
      remark: editForm.remark || '',
      photo: editForm.photo || ''
    }
    await request.put(`/user/customers/${c.id}`, payload)
    // 只回填本次编辑的字段：接口返回的是裸客户对象，没有 lastFollowUpTime 等列表补充字段，
    // 整体覆盖会把"未跟进天数"弄丢
    Object.assign(c, payload)
    editVisible.value = false
    MessagePlugin.success('客户资料已更新')
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || ''
    MessagePlugin.error(/不存在/.test(msg) ? '仅客户归属人可以修改资料' : (msg || '保存失败'))
  } finally {
    editSaving.value = false
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/customers')
}

onMounted(() => {
  fetchList()
  document.addEventListener('paste', onDocumentPaste)
})
onUnmounted(() => {
  document.removeEventListener('paste', onDocumentPaste)
  // 释放缓存引用，避免长时间浏览后堆积
  Object.keys(followCache).forEach(k => delete followCache[k])
})
</script>

<style scoped>
.feed-page {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: #0d0f13;
  display: flex;
  flex-direction: column;
  /* 深色主题：覆盖全局文字/主题色变量，模板里的 var(--color-*) 自动跟随 */
  --color-text-primary: #f2f4f7;
  --color-text-secondary: #a9b1bd;
  --color-text-tertiary: #6f7886;
  --color-primary: #4c8dff;
  color: var(--color-text-primary);
}
.feed-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 48px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
  background: linear-gradient(to bottom, rgba(13, 15, 19, 0.96), rgba(13, 15, 19, 0));
}
.feed-icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  outline: none;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.08);
  color: #e8ebf0;
}
.feed-scroller {
  flex: 1;
  overflow-y: auto;
  scroll-snap-type: y mandatory;
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  outline: none;
}
.feed-scroller::-webkit-scrollbar {
  display: none;
}
.feed-slide {
  height: 100%;
  scroll-snap-align: start;
  scroll-snap-stop: always;
  box-sizing: border-box;
  padding: 52px 12px 6px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.feed-card {
  background: #161a21;
  border: 1px solid #232833;
  border-radius: 6px;
  padding: 14px;
  flex-shrink: 0;
}
.feed-follows {
  background: #161a21;
  border: 1px solid #232833;
  border-radius: 6px;
  padding: 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior-y: contain;
}
.feed-hint {
  flex-shrink: 0;
  text-align: center;
  font-size: 11px;
  color: var(--color-text-tertiary);
  padding-bottom: 2px;
}
/* 跟进图片：显式尺寸，不依赖工具类，避免图片按原始大小撑爆一屏 */
.follow-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}
.follow-photo {
  display: block;
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #2a303a;
  background: #1b2029;
  cursor: pointer;
}
.follow-photo-more {
  width: 56px;
  height: 56px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 加载态文字在深色下可读 */
.feed-page :deep(.t-loading__text) {
  color: #a9b1bd;
}

/* 增加跟进弹窗（Teleport 到 body：class 落在外层容器，需用后代选择器命中 .t-dialog） */
:global(.feed-dark-dialog .t-dialog) {
  background: #161a21;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.55);
  --td-bg-color-container: #161a21;
}
:global(.feed-dark-dialog .t-dialog__header),
:global(.feed-dark-dialog .t-dialog__header-content) {
  color: #f2f4f7;
}
:global(.feed-dark-dialog .t-dialog__body) {
  color: #a9b1bd;
}
:global(.feed-dark-dialog .t-dialog__close) {
  color: #a9b1bd;
}
:global(.feed-dark-dialog .t-textarea__inner) {
  background: #1b2029;
  border-color: #2a303a;
  color: #f2f4f7;
}
:global(.feed-dark-dialog .t-textarea__inner::placeholder) {
  color: #6f7886;
}
:global(.feed-dark-dialog .t-input) {
  background: #1b2029;
  border-color: #2a303a;
}
:global(.feed-dark-dialog .t-input .t-input__inner) {
  color: #f2f4f7;
}
:global(.feed-dark-dialog .t-input .t-input__inner::placeholder) {
  color: #6f7886;
}
:global(.feed-dark-dialog .t-input.t-is-focused) {
  border-color: #4c8dff;
  box-shadow: none;
}
.feed-footer {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  background: rgba(13, 15, 19, 0.94);
  border-top: 1px solid #232833;
  backdrop-filter: blur(8px);
}
/* 深色下描边按钮默认是白底黑字，改成透明底浅色描边 */
.feed-footer :deep(.t-button--variant-outline) {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.22);
  color: #e8ebf0;
}
.feed-footer :deep(.t-button--variant-outline:hover) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.32);
  color: #fff;
}
.feed-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 头像上传区：虚线框提示位，点击选图 + Ctrl+V 粘贴 */
.avatar-paste-zone {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border: 1px dashed #3a4150;
  border-radius: 6px;
  color: var(--color-text-tertiary);
  background: rgba(255, 255, 255, 0.02);
  cursor: pointer;
  transition: border-color 0.15s, color 0.15s;
}
.avatar-paste-zone:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.avatar-paste-zone.is-busy {
  opacity: 0.6;
  pointer-events: none;
}
.avatar-clear {
  border: 0;
  outline: none;
  background: transparent;
  padding: 0;
  font-size: 11px;
  color: #f87171;
  cursor: pointer;
}
.avatar-clear:hover {
  text-decoration: underline;
}
</style>
