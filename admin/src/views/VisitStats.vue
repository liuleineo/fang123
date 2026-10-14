<template>
  <div class="visit-stats">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h2 class="text-lg font-semibold text-[var(--color-text-primary)]">访问统计</h2>
        <p class="text-xs text-[var(--color-text-tertiary)] mt-1">
          {{ data.toDate }} 数据每小时汇总一次，实时在线为最近 5 分钟有活跃的访客数
        </p>
      </div>
      <t-select v-model="days" :options="dayOptions" style="width: 140px" @change="load" />
    </div>

    <!-- 概览 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-6">
      <div class="bg-white rounded-xl p-5 border border-gray-50">
        <div class="flex items-start justify-between mb-3">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-blue-50">
            <Eye class="w-5 h-5 text-blue-500" />
          </div>
          <span class="text-xs font-medium px-2 py-1 rounded-full" :class="trendClass(overview.todayPv, overview.yesterdayPv)">
            {{ trendText(overview.todayPv, overview.yesterdayPv) }}
          </span>
        </div>
        <p class="text-2xl font-bold text-[var(--color-text-primary)] mb-1">{{ overview.todayPv ?? 0 }}</p>
        <p class="text-sm text-[var(--color-text-tertiary)]">今日访问量(PV) <span class="text-xs ml-1">昨日 {{ overview.yesterdayPv ?? 0 }}</span></p>
      </div>

      <div class="bg-white rounded-xl p-5 border border-gray-50">
        <div class="flex items-start justify-between mb-3">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-green-50">
            <UserRound class="w-5 h-5 text-green-600" />
          </div>
          <span class="text-xs font-medium px-2 py-1 rounded-full bg-green-50 text-green-500">
            新访客 {{ overview.todayNewUv ?? 0 }}
          </span>
        </div>
        <p class="text-2xl font-bold text-[var(--color-text-primary)] mb-1">{{ overview.todayUv ?? 0 }}</p>
        <p class="text-sm text-[var(--color-text-tertiary)]">今日独立访客(UV) <span class="text-xs ml-1">昨日 {{ overview.yesterdayUv ?? 0 }}</span></p>
      </div>

      <div class="bg-white rounded-xl p-5 border border-gray-50">
        <div class="flex items-start justify-between mb-3">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-orange-50">
            <Layers class="w-5 h-5 text-orange-500" />
          </div>
        </div>
        <p class="text-2xl font-bold text-[var(--color-text-primary)] mb-1">{{ overview.todaySessions ?? 0 }}</p>
        <p class="text-sm text-[var(--color-text-tertiary)]">今日会话数 <span class="text-xs ml-1">人均 {{ pagesPerSession }}</span></p>
      </div>

      <div class="bg-white rounded-xl p-5 border border-gray-50">
        <div class="flex items-start justify-between mb-3">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center bg-purple-50">
            <Activity class="w-5 h-5 text-purple-600" />
          </div>
        </div>
        <p class="text-2xl font-bold text-[var(--color-text-primary)] mb-1">{{ overview.online ?? 0 }}</p>
        <p class="text-sm text-[var(--color-text-tertiary)]">实时在线 <span class="text-xs ml-1">平均停留 {{ formatDuration(overview.todayAvgDurationMs) }}</span></p>
      </div>
    </div>

    <!-- 趋势 -->
    <div class="bg-white rounded-xl border border-gray-50 p-6 mb-6">
      <div class="flex items-center justify-between mb-5">
        <h3 class="text-base font-medium text-[var(--color-text-primary)]">访问趋势</h3>
        <div class="flex items-center gap-4 text-xs text-[var(--color-text-tertiary)]">
          <span class="flex items-center gap-1"><i class="w-2.5 h-2.5 rounded-sm bg-blue-400 inline-block"></i>PV</span>
          <span class="flex items-center gap-1"><i class="w-2.5 h-2.5 rounded-sm bg-green-400 inline-block"></i>UV</span>
        </div>
      </div>
      <div v-if="loading" class="h-40 flex items-center justify-center text-sm text-[var(--color-text-tertiary)]">加载中…</div>
      <div v-else-if="!trend.length" class="h-40 flex items-center justify-center text-sm text-[var(--color-text-tertiary)]">
        暂无数据，访问统计会在用户打开网站后自动累积
      </div>
      <template v-else>
        <div class="flex items-end gap-[2px] h-40">
          <div v-for="item in trend" :key="item.date" class="flex-1 h-full flex flex-col justify-end">
            <div class="flex items-end justify-center gap-[1px] h-full">
              <div class="w-1/2 max-w-[14px] bg-blue-400 rounded-t transition-all"
                   :style="{ height: barHeight(item.pv) }"
                   :title="`${item.date} PV ${item.pv} / UV ${item.uv}`"></div>
              <div class="w-1/2 max-w-[14px] bg-green-400 rounded-t transition-all"
                   :style="{ height: barHeight(item.uv) }"
                   :title="`${item.date} UV ${item.uv} / 新访客 ${item.newUv}`"></div>
            </div>
          </div>
        </div>
        <div class="flex justify-between text-xs text-[var(--color-text-tertiary)] mt-3">
          <span>{{ data.fromDate }}</span>
          <span>{{ data.toDate }}</span>
        </div>
      </template>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- 平台分布 -->
      <div class="bg-white rounded-xl border border-gray-50 p-6">
        <h3 class="text-base font-medium text-[var(--color-text-primary)] mb-4">平台分布</h3>
        <div v-if="!platforms.length" class="text-sm text-[var(--color-text-tertiary)] py-6 text-center">暂无数据</div>
        <div v-else class="space-y-4">
          <div v-for="item in platforms" :key="item.platform">
            <div class="flex items-center justify-between text-sm mb-1">
              <span class="text-[var(--color-text-primary)]">{{ platformLabel(item.platform) }}</span>
              <span class="text-[var(--color-text-tertiary)] text-xs">PV {{ item.pv }} · UV {{ item.uv }}</span>
            </div>
            <div class="h-2 rounded-full bg-gray-100 overflow-hidden">
              <div class="h-full bg-blue-400 rounded-full" :style="{ width: percent(item.pv, platformTotal) }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 热门页面 -->
      <div class="lg:col-span-2 bg-white rounded-xl border border-gray-50 p-6">
        <h3 class="text-base font-medium text-[var(--color-text-primary)] mb-4">热门页面 TOP 20</h3>
        <div v-if="!topPages.length" class="text-sm text-[var(--color-text-tertiary)] py-6 text-center">暂无数据</div>
        <t-table v-else row-key="route" size="small" :data="topPages" :columns="pageColumns" :pagination="null" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Eye, UserRound, Layers, Activity } from 'lucide-vue-next'
import request from '@/utils/request'

const loading = ref(false)
const days = ref(30)
const dayOptions = [
  { label: '近 7 天', value: 7 },
  { label: '近 30 天', value: 30 },
  { label: '近 90 天', value: 90 }
]

const overview = reactive({
  todayPv: 0, todayUv: 0, todaySessions: 0, todayNewUv: 0,
  todayAvgDurationMs: 0, yesterdayPv: 0, yesterdayUv: 0, online: 0
})
const trend = ref([])
const platforms = ref([])
const topPages = ref([])
const data = reactive({ fromDate: '', toDate: '' })

const pageColumns = [
  { colKey: 'route', title: '页面', ellipsis: true },
  { colKey: 'pv', title: 'PV', width: 90 },
  { colKey: 'uv', title: 'UV', width: 90 },
  { colKey: 'avgDuration', title: '平均停留', width: 120 }
]

const maxValue = computed(() => {
  const values = trend.value.map(t => Math.max(Number(t.pv) || 0, Number(t.uv) || 0))
  return Math.max(1, ...values)
})

const platformTotal = computed(() =>
  platforms.value.reduce((sum, item) => sum + (Number(item.pv) || 0), 0)
)

const pagesPerSession = computed(() => {
  const sessions = Number(overview.todaySessions) || 0
  if (!sessions) return '-'
  return ((Number(overview.todayPv) || 0) / sessions).toFixed(1) + ' 页'
})

function barHeight(value) {
  const ratio = (Number(value) || 0) / maxValue.value
  if (!ratio) return '2px'
  return Math.max(2, Math.round(ratio * 100)) + '%'
}

function percent(value, total) {
  if (!total) return '0%'
  return Math.max(2, Math.round((Number(value) || 0) / total * 100)) + '%'
}

function platformLabel(platform) {
  const map = { web: 'Web 网站', mini: '微信小程序', android: 'Android', ios: 'iOS', harmony: 'HarmonyOS', all: '全部' }
  return map[platform] || platform
}

function formatDuration(ms) {
  const value = Number(ms) || 0
  if (!value) return '-'
  if (value < 1000) return value + ' ms'
  const seconds = Math.round(value / 1000)
  if (seconds < 60) return seconds + ' 秒'
  return Math.floor(seconds / 60) + ' 分 ' + (seconds % 60) + ' 秒'
}

function trendText(today, yesterday) {
  const current = Number(today) || 0
  const previous = Number(yesterday) || 0
  if (!previous) return current ? '新数据' : '-'
  const diff = Math.round((current - previous) / previous * 100)
  return (diff >= 0 ? '+' : '') + diff + '%'
}

function trendClass(today, yesterday) {
  const current = Number(today) || 0
  const previous = Number(yesterday) || 0
  if (!previous) return 'bg-gray-50 text-gray-500'
  return current >= previous ? 'bg-green-50 text-green-500' : 'bg-red-50 text-red-500'
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/admin/visit/dashboard', { params: { days: days.value } })
    Object.assign(overview, res.overview || {})
    trend.value = (res.trend || []).map(item => ({
      ...item,
      avgDuration: formatDuration(item.avgDurationMs)
    }))
    platforms.value = res.platforms || []
    topPages.value = (res.topPages || []).map(item => ({
      ...item,
      avgDuration: formatDuration(item.avgDurationMs)
    }))
    data.fromDate = res.fromDate || ''
    data.toDate = res.toDate || ''
  } catch (e) {
    console.error('加载访问统计失败:', e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
