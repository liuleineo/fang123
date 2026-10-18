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
      <UsersRound class="w-12 h-12 text-gray-200 mb-3" />
      <p class="text-sm text-[var(--color-text-tertiary)]">还没有可跟进的客户</p>
      <t-button class="mt-4" theme="primary" @click="goBack">返回</t-button>
    </div>

    <!-- 垂直信息流：每屏一个客户，上下滑动切换 -->
    <div v-else ref="scrollerRef" class="feed-scroller" tabindex="0" @scroll.passive="onScroll">
      <section v-for="(c, i) in list" :key="c.id" class="feed-slide">
        <!-- 客户资料 -->
        <div class="feed-card">
          <div class="flex items-start gap-3">
            <div class="w-14 h-14 rounded-[6px] overflow-hidden flex-shrink-0 bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center">
              <img v-if="c.photo" :src="c.photo" class="w-full h-full object-cover" @error="e => (e.target.style.display = 'none')" />
              <UserIcon v-else class="w-7 h-7 text-[var(--color-primary)]" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <h2 class="text-lg font-bold text-[var(--color-text-primary)] truncate">{{ c.name || '未命名客户' }}</h2>
                <span v-if="c.intention" :class="['text-[11px] px-1.5 py-0.5 rounded font-medium', intentionClass(c.intention)]">{{ c.intention }}意向</span>
                <span v-if="c.sharedTo" class="text-[11px] px-1.5 py-0.5 rounded bg-gray-100 text-[var(--color-text-tertiary)]">共享</span>
              </div>
              <p class="mt-1 text-base font-semibold tracking-wide text-[var(--color-text-primary)]">{{ c.phone || '未填写手机号' }}</p>
              <p class="mt-1 text-xs text-[var(--color-text-tertiary)]">
                <span :class="idleClass(c)">{{ idleText(c) }}</span>
              </p>
            </div>
          </div>

          <div v-if="c.remark" class="mt-3 text-sm leading-6 text-[var(--color-text-secondary)] bg-[#F8FAFE] rounded-[4px] px-3 py-2 whitespace-pre-wrap max-h-[110px] overflow-y-auto overscroll-y-contain">
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
            <div v-for="f in followState(c.id).items.slice(0, 5)" :key="f.id" class="bg-[#F8FAFE] rounded-[4px] px-3 py-2">
              <div class="flex items-center gap-2 mb-1">
                <span v-if="f.method" class="text-[11px] px-1.5 py-0.5 rounded bg-white text-[var(--color-primary)] border border-blue-100">{{ f.method }}</span>
                <span class="text-[11px] text-[var(--color-text-tertiary)] ml-auto">{{ fmtTime(f.followUpTime || f.createdAt) }}</span>
              </div>
              <p class="text-sm leading-5 text-[var(--color-text-secondary)] whitespace-pre-wrap line-clamp-3">{{ f.content }}</p>
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

    <!-- 增加跟进弹窗 -->
    <t-dialog v-model:visible="followVisible" header="增加跟进" width="360px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-3">
        <p class="text-sm text-[var(--color-text-secondary)]">
          客户：<span class="font-medium text-[var(--color-text-primary)]">{{ current?.name || '' }}</span>
        </p>
        <div class="flex items-center gap-1.5 flex-wrap">
          <button v-for="m in followMethods" :key="m" type="button"
            :class="['px-3 py-1 rounded-full text-xs font-medium border-0 outline-none transition-colors',
              followMethod === m ? 'bg-[var(--color-primary)] text-white' : 'bg-gray-100 text-[var(--color-text-secondary)]']"
            @click="followMethod = m">{{ m }}</button>
        </div>
        <t-textarea v-model="followContent" placeholder="记录本次沟通内容，如：客户想了解89㎡户型，预算300万左右" :autosize="{ minRows: 3, maxRows: 6 }" />
        <t-button block theme="primary" :loading="followSaving" @click="submitFollow">保存跟进</t-button>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { MessagePlugin } from 'tdesign-vue-next'
import { ChevronLeft, Phone, MessageSquarePlus, UsersRound, User as UserIcon } from 'lucide-vue-next'
import request from '@/utils/request'

const router = useRouter()

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 20
const loading = ref(false)
const currentIndex = ref(0)
const scrollerRef = ref(null)
const hasMore = computed(() => list.value.length < total.value)
const current = computed(() => list.value[currentIndex.value] || null)

/** 客户列表：按"最久未跟进"排序，方便信息流逐个过客户 */
async function fetchList(reset = false) {
  if (loading.value) return
  if (reset) { page.value = 1; list.value = []; total.value = 0 }
  loading.value = true
  try {
    const r = await request.get('/user/customers', {
      params: { page: page.value, size: pageSize, sortField: 'lastFollowUpTime', sortOrder: 'asc' }
    })
    const records = r?.records || []
    list.value = reset ? records : [...list.value, ...records]
    total.value = Number(r?.total || 0)
    page.value += 1
    await nextTick()
    if (reset) scrollerRef.value?.scrollTo({ top: 0 })
    ensureFollows(currentIndex.value)
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
  if (d === null || d >= 7) return 'text-red-500 font-medium'
  if (d >= 3) return 'text-orange-500'
  return 'text-[var(--color-text-tertiary)]'
}
function intentionClass(v) {
  if (v === '高') return 'bg-red-50 text-red-500'
  if (v === '中') return 'bg-orange-50 text-orange-500'
  return 'bg-gray-100 text-[var(--color-text-tertiary)]'
}
function fmtTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
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

function openFollow() {
  if (!current.value) return
  followContent.value = ''
  followMethod.value = '电话'
  followVisible.value = true
}

async function submitFollow() {
  const content = followContent.value.trim()
  if (!content) { MessagePlugin.warning('请填写跟进内容'); return }
  followSaving.value = true
  try {
    const created = await request.post(`/user/customers/${current.value.id}/follow-ups`, {
      content,
      method: followMethod.value
    })
    // 同步当前客户卡片上的"最后跟进"，并刷新该客户记录
    const c = current.value
    c.lastFollowUpTime = created?.followUpTime || created?.createdAt || new Date().toISOString()
    followCache[c.id] = { items: [], loaded: false, loading: false }
    await loadFollows(c)
    followVisible.value = false
    followContent.value = ''
    MessagePlugin.success('已添加跟进')
  } catch (e) {
    MessagePlugin.error(e?.message || '添加失败')
  } finally {
    followSaving.value = false
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/customers')
}

onMounted(() => {
  fetchList(true)
})
onUnmounted(() => {
  // 释放缓存引用，避免长时间浏览后堆积
  Object.keys(followCache).forEach(k => delete followCache[k])
})
</script>

<style scoped>
.feed-page {
  position: fixed;
  inset: 0;
  z-index: 60;
  background: #f8fafe;
  display: flex;
  flex-direction: column;
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
  background: linear-gradient(to bottom, rgba(248, 250, 254, 0.96), rgba(248, 250, 254, 0));
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
  background: rgba(255, 255, 255, 0.9);
  color: var(--color-text-primary);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
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
  background: #fff;
  border: 1px solid #f0f2f5;
  border-radius: 6px;
  padding: 14px;
  flex-shrink: 0;
}
.feed-follows {
  background: #fff;
  border: 1px solid #f0f2f5;
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
.feed-footer {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid #f0f2f5;
  backdrop-filter: blur(8px);
}
.feed-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>
