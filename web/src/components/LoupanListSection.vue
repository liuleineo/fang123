<template>
  <div class="loupan-list-section">
    <!-- 筛选栏 -->
    <section class="py-3 border-b border-gray-50 bg-white sticky top-[var(--header-height)] z-40">
      <div class="section-container flex flex-wrap items-center gap-2">
        <span class="text-xs text-[var(--color-text-secondary)] font-medium whitespace-nowrap">筛选条件</span>
        <t-select
          v-model="filterDistrict" placeholder="行政区" clearable size="small" class="w-[120px]"
          :options="districtOpts" @change="doSearch"
        />
        <t-select
          v-model="filterPlate" placeholder="板块" clearable size="small" class="w-[120px]"
          :options="plateOpts" @change="doSearch"
        />
        <t-select
          v-model="filterHouseType" placeholder="楼盘类型" clearable size="small" class="w-[110px]"
          :options="houseTypeOpts" @change="doSearch"
        />
        <t-input
          v-if="showKeyword"
          v-model="keyword"
          placeholder="搜索楼盘 / 开发商"
          clearable
          size="small"
          class="!w-[180px]"
          @enter="doSearch"
          @clear="doSearch"
        >
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <span class="text-xs text-[var(--color-text-tertiary)] ml-auto">共 {{ total }} 个楼盘</span>
      </div>
    </section>

    <!-- 楼盘列表 -->
    <section id="list" class="py-10 bg-[#F8FAFE] min-h-[50vh]">
      <div class="section-container">
        <!-- 板块模式提示条 -->
        <div v-if="spotlightType" class="flex flex-wrap items-center gap-2 mb-4">
          <span
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-semibold"
            :class="spotlightType === 'presale' ? 'bg-amber-50 text-amber-600' : 'bg-blue-50 text-[var(--color-primary)]'"
          >
            <component :is="spotlightType === 'presale' ? Megaphone : Rocket" class="w-3.5 h-3.5" />
            {{ spotlightType === 'presale' ? '预售公示' : '最新开盘' }}
          </span>
          <span class="text-xs text-[var(--color-text-tertiary)]">共 {{ total }} 个楼盘</span>
          <button
            class="ml-auto text-xs text-[var(--color-primary)] hover:underline"
            @click="clearSpotlight"
          >返回全部楼盘</button>
        </div>

        <div v-if="loading" class="flex justify-center py-20">
          <t-loading size="large" text="加载中..." />
        </div>

        <div v-else-if="!loupanList.length" class="text-center py-20">
          <Building2 class="w-16 h-16 text-gray-200 mx-auto mb-4" />
          <p class="text-[var(--color-text-tertiary)]">{{ emptyText }}</p>
        </div>

        <div v-else class="grid grid-cols-2 gap-3 lg:grid-cols-3 sm:gap-6">
          <router-link
            v-for="lp in loupanList"
            :key="lp.id"
            :to="`/loupan/${lp.encodedId}`"
            class="bg-white rounded-2xl border border-gray-100 overflow-hidden card-hover group block"
          >
            <!-- 封面图 -->
            <div class="h-36 sm:h-48 bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center relative overflow-hidden">
              <t-image v-if="lp.coverImage" :src="lp.coverImage" fit="cover" class="w-full h-full absolute inset-0" />
              <Building2 v-else class="w-16 h-16 text-blue-300" />
              <!-- 状态标签 -->
              <span
                :class="['absolute top-3 left-3 px-2.5 py-1 rounded-full text-xs font-medium',
                  lp.salesStatus===1?'bg-green-500 text-white':
                  lp.salesStatus===2?'bg-gray-400 text-white':
                  lp.salesStatus===3?'bg-blue-500 text-white':
                  'bg-gray-300 text-gray-600']"
              >
                {{ ['待售','在售','售罄','已交付'][lp.salesStatus] || '未知' }}
              </span>
              <!-- 装修类型 -->
              <span class="absolute top-3 right-3 px-2 py-0.5 rounded text-xs bg-white/80 text-[var(--color-text-secondary)]">
                {{ ['','精装','毛坯','简装'][lp.decorateType] || '' }}
              </span>
            </div>
            <div class="p-3 sm:p-5">
              <!-- 标题行：楼盘名 + 开发商品牌占满整行，避免手机双列时互相挤压截断 -->
              <h3 class="mb-1 text-sm sm:text-base font-bold text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)] transition-colors leading-snug flex items-center gap-1.5">
                <span class="min-w-0 truncate">{{ lp.projectName }}</span>
                <span v-if="lp.brandList" class="flex-shrink-0 px-2 py-0.5 rounded text-xs font-medium text-white bg-orange-500 whitespace-nowrap">{{ lp.brandList }}</span>
              </h3>
              <!-- 价格 + 位置行：手机双列时各自完整显示 -->
              <div v-if="lp.avgUnitPrice || lp.district" class="mb-2 flex items-center justify-between gap-2">
                <span v-if="lp.avgUnitPrice" class="shrink-0 text-xs sm:text-sm font-bold text-[var(--color-danger)] whitespace-nowrap">{{ lp.avgUnitPrice }}元/㎡</span>
                <span v-if="lp.district" class="min-w-0 flex items-center gap-0.5 text-xs text-[var(--color-text-tertiary)] truncate">
                  <MapPin class="w-3 h-3 shrink-0 -mt-px" />{{ lp.district }}{{ lp.plate ? '·' + lp.plate : '' }}
                </span>
              </div>
              <div v-if="lp.avgUnitPriceYangfang||lp.avgUnitPriceDieshu||lp.avgUnitPricePaiwu" class="flex flex-wrap gap-1.5 mb-3">
                <span v-if="lp.avgUnitPriceYangfang" class="px-2 py-0.5 text-xs rounded bg-gray-50 text-[var(--color-text-secondary)]">洋房 {{ lp.avgUnitPriceYangfang }}元/㎡</span>
                <span v-if="lp.avgUnitPriceDieshu" class="px-2 py-0.5 text-xs rounded bg-gray-50 text-[var(--color-text-secondary)]">叠墅 {{ lp.avgUnitPriceDieshu }}元/㎡</span>
                <span v-if="lp.avgUnitPricePaiwu" class="px-2 py-0.5 text-xs rounded bg-gray-50 text-[var(--color-text-secondary)]">排屋 {{ lp.avgUnitPricePaiwu }}元/㎡</span>
              </div>
              <div class="flex flex-wrap gap-1.5 mb-3">
                <span class="px-2 py-0.5 text-xs rounded bg-gray-50 text-[var(--color-text-secondary)]">
                  建面{{ lp.areaMin }}-{{ lp.areaMax }}㎡
                </span>
                <span v-if="lp.houseType" class="px-2 py-0.5 text-xs rounded bg-blue-50 text-[var(--color-primary)]">
                  {{ ['','住宅','公寓','商铺','别墅'][lp.houseType] || '' }}
                </span>
                <span v-if="lp.buildingTotal" class="px-2 py-0.5 text-xs rounded bg-gray-50 text-[var(--color-text-secondary)]">
                  {{ lp.buildingTotal }}栋
                </span>
              </div>
              <!-- 板块模式 / 首页分组：展示来源标签与预售证关键日期 -->
              <div v-if="(spotlightType || lp.group) && (lp.group || lp.publicityDate || lp.issueDate)" class="flex flex-wrap gap-1.5">
                <span v-if="lp.group === 'presale'" class="px-2 py-0.5 text-xs rounded bg-amber-50 text-amber-600">预售公示</span>
                <span v-if="lp.group === 'opening'" class="px-2 py-0.5 text-xs rounded bg-blue-50 text-[var(--color-primary)]">最新开盘</span>
                <span v-if="lp.group === 'sort'" class="px-2 py-0.5 text-xs rounded bg-orange-50 text-orange-600">热门推荐</span>
                <span v-if="(spotlightType === 'presale' || lp.group === 'presale') && lp.publicityDate" class="px-2 py-0.5 text-xs rounded bg-amber-50 text-amber-600">
                  公示 {{ lp.publicityDate }}
                </span>
                <span v-if="lp.issueDate" class="px-2 py-0.5 text-xs rounded bg-blue-50 text-[var(--color-primary)]">
                  核发 {{ lp.issueDate }}
                </span>
              </div>
            </div>
          </router-link>
        </div>

        <!-- 分页 -->
        <div v-if="total > pg.pageSize" class="flex justify-center mt-10">
          <t-pagination
            v-model:current="pg.current"
            :total="total"
            :page-size="pg.pageSize"
            size="medium"
            @change="onPageChange"
          />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Search, Building2, MapPin, Megaphone, Rocket } from 'lucide-vue-next'
import request from '@/utils/request'

const props = defineProps({
  /** 当前板块模式：'' | 'presale' | 'opening'，与父组件双向绑定 */
  spotlight: { type: String, default: '' },
  /** 是否在筛选栏显示关键词搜索框（独立页面用） */
  showKeyword: { type: Boolean, default: false },
  /** 首页模式：默认列表按「预售公示 → 最新开盘 → 排序权重(>0)」排列 */
  homeOrder: { type: Boolean, default: false }
})
const emit = defineEmits(['update:spotlight'])

const spotlightType = computed({
  get: () => props.spotlight,
  set: (v) => emit('update:spotlight', v)
})

const loupanList = ref([])
const loading = ref(false)
const total = ref(0)
const keyword = ref('')
const filterDistrict = ref('')
const filterPlate = ref('')
const filterHouseType = ref(null)
const districtOpts = ref([])
const plateOpts = ref([])

const pg = reactive({ current: 1, pageSize: 12 })

const houseTypeOpts = [
  { label: '住宅', value: 1 }, { label: '公寓', value: 2 }, { label: '商铺', value: 3 }, { label: '别墅', value: 4 }
]

/** 是否处于筛选状态（有筛选时不走首页分组排序） */
const hasFilter = computed(() => !!(keyword.value || filterDistrict.value || filterPlate.value || filterHouseType.value))

const emptyText = computed(() => {
  if (spotlightType.value === 'presale') return '暂无公示中的楼盘'
  if (spotlightType.value === 'opening') return '近 7 天暂无新取证楼盘'
  return '暂无符合条件的楼盘'
})

async function fetchData() {
  if (spotlightType.value) return fetchSpotlight()
  return fetchLoupanList()
}

/** 普通列表（含首页分组模式）；筛选条件变化时都走这里 */
async function fetchLoupanList() {
  loading.value = true
  try {
    const p = { page: pg.current, size: pg.pageSize }
    // 首页默认视图：预售公示 → 最新开盘 → 排序权重(>0) 从高到低
    if (props.homeOrder && !hasFilter.value) {
      const r = await request.get('/public/loupan-home', { params: p })
      loupanList.value = r?.records || []
      total.value = r?.total || 0
      return
    }
    p.salesStatus = '1'
    if (keyword.value) p.keyword = keyword.value
    if (filterDistrict.value) p.district = filterDistrict.value
    if (filterPlate.value) p.plate = filterPlate.value
    if (filterHouseType.value) p.houseType = filterHouseType.value
    const r = await request.get('/public/loupans', { params: p })
    loupanList.value = r?.records || []
    total.value = r?.total || 0
  } catch {} finally { loading.value = false }
}

async function fetchFilters() {
  try {
    const r = await request.get('/public/loupan-filters')
    if (r?.districts) districtOpts.value = r.districts.map(d => ({ label: d, value: d }))
    if (r?.plates) plateOpts.value = r.plates.map(p => ({ label: p, value: p }))
  } catch {}
}

/**
 * 预售公示 / 最新开盘 楼盘列表
 *
 * @param type 板块类型；必须显式传入。spotlightType 是依赖父组件 props 的 computed，
 *             刚 emit 更新时同一 tick 内仍读到旧值（''），会导致请求 type 为空、后端报
 *             「type 仅支持 presale / opening」。
 */
async function fetchSpotlight(type = spotlightType.value) {
  loading.value = true
  try {
    const r = await request.get('/public/loupan-spotlight', {
      params: { type, page: pg.current, size: pg.pageSize }
    })
    loupanList.value = r?.records || []
    total.value = r?.total || 0
  } catch {} finally { loading.value = false }
}

/** 手动搜索/筛选：退出板块模式，回到第一页（直接请求普通列表，不读刚被重置的 computed） */
function doSearch() {
  spotlightType.value = ''
  pg.current = 1
  fetchLoupanList()
}

/** 供父组件调用：带关键词搜索 */
function search(kw) {
  if (kw !== undefined) keyword.value = kw
  doSearch()
}

function toggleSpotlight(type) {
  if (spotlightType.value === type) return clearSpotlight()
  spotlightType.value = type
  pg.current = 1
  fetchSpotlight(type)
  document.getElementById('list')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function clearSpotlight() {
  spotlightType.value = ''
  pg.current = 1
  fetchLoupanList()
}

function onPageChange() {
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchFilters()
})

defineExpose({ search, toggleSpotlight, clearSpotlight, refresh: fetchData, total })
</script>
