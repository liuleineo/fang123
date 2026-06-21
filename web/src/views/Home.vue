<template>
  <div class="home-page">
    <!-- Hero 搜索区 -->
    <section class="relative overflow-hidden bg-gradient-to-br from-[#F0F5FF] via-white to-[#E8F2FF] py-16 lg:py-24">
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div class="absolute -top-40 -right-40 w-96 h-96 bg-[#0052D9]/5 rounded-full blur-3xl" />
        <div class="absolute -bottom-40 -left-40 w-80 h-80 bg-[#618DFF]/5 rounded-full blur-3xl" />
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gradient-to-br from-blue-50/50 to-transparent rounded-full blur-3xl" />
      </div>

      <div class="section-container relative z-10 text-center">
        <h1 class="text-4xl sm:text-5xl lg:text-6xl font-bold text-[var(--color-text-primary)] leading-tight mb-6">
          找到属于你的<br class="sm:hidden" />
          <span class="bg-gradient-to-r from-[#0052D9] to-[#618DFF] bg-clip-text text-transparent">理想家园</span>
        </h1>
        <p class="text-base sm:text-lg text-[var(--color-text-tertiary)] max-w-xl mx-auto mb-10 leading-relaxed">
          覆盖杭州全域楼盘，实时掌握开盘、价格、户型动态
        </p>

        <!-- 搜索栏 -->
        <div class="max-w-2xl mx-auto bg-white rounded-2xl shadow-lg border border-gray-100 p-3 flex flex-col sm:flex-row gap-3">
          <t-input
            v-model="searchKeyword"
            placeholder="搜索楼盘名称、开发商、品牌..."
            size="large"
            clearable
            class="flex-1"
            @enter="doSearch"
            @clear="doSearch"
          >
            <template #prefix-icon><Search class="w-5 h-5 text-[var(--color-text-tertiary)]" /></template>
          </t-input>
          <t-button theme="primary" size="large" class="!px-8 !rounded-xl" @click="doSearch">
            <Search class="w-4 h-4 mr-1.5" />搜索楼盘
          </t-button>
        </div>

        <!-- 热门标签 -->
        <div class="flex flex-wrap items-center justify-center gap-2 mt-5">
          <span class="text-xs text-[var(--color-text-tertiary)]">热门：</span>
          <button
            v-for="tag in hotTags"
            :key="tag"
            class="px-3 py-1 text-xs bg-white border border-gray-200 rounded-full hover:border-[var(--color-primary)] hover:text-[var(--color-primary)] transition-colors"
            @click="searchKeyword = tag; doSearch()"
          >{{ tag }}</button>
        </div>
      </div>
    </section>

    <!-- 筛选栏 -->
    <section class="py-6 border-b border-gray-50 bg-white sticky top-[var(--header-height)] z-40">
      <div class="section-container flex flex-wrap items-center gap-3">
        <span class="text-sm text-[var(--color-text-secondary)] font-medium whitespace-nowrap">筛选条件</span>
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
        <t-select
          v-model="filterDecorate" placeholder="装修" clearable size="small" class="w-[100px]"
          :options="decorateOpts" @change="doSearch"
        />
        <span class="text-xs text-[var(--color-text-tertiary)] ml-auto">共 {{ total }} 个楼盘</span>
      </div>
    </section>

    <!-- 楼盘列表 -->
    <section id="list" class="py-10 bg-[#F8FAFE] min-h-[50vh]">
      <div class="section-container">
        <div v-if="loading" class="flex justify-center py-20">
          <t-loading size="large" text="加载中..." />
        </div>

        <div v-else-if="!loupanList.length" class="text-center py-20">
          <Building2 class="w-16 h-16 text-gray-200 mx-auto mb-4" />
          <p class="text-[var(--color-text-tertiary)]">暂无符合条件的楼盘</p>
        </div>

        <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <router-link
            v-for="lp in loupanList"
            :key="lp.id"
            :to="`/loupan/${lp.encodedId}`"
            class="bg-white rounded-2xl border border-gray-100 overflow-hidden card-hover group block"
          >
            <!-- 封面图 -->
            <div class="h-48 bg-gradient-to-br from-blue-50 to-blue-100 flex items-center justify-center relative overflow-hidden">
              <t-image v-if="lp.coverImage" :src="lp.coverImage" fit="cover" class="w-full h-full absolute inset-0" />
              <Building2 v-else class="w-16 h-16 text-blue-300" />
              <!-- 状态标签 -->
              <span
                :class="['absolute top-3 left-3 px-2.5 py-1 rounded-full text-xs font-medium',
                  lp.salesStatus===1?'bg-green-500 text-white':
                  lp.salesStatus===2?'bg-gray-400 text-white':
                  lp.salesStatus===3?'bg-orange-500 text-white':
                  lp.salesStatus===4?'bg-blue-500 text-white':
                  'bg-gray-300 text-gray-600']"
              >
                {{ ['待开盘','在售','售罄','停工','已交付'][lp.salesStatus] || '未知' }}
              </span>
              <!-- 装修类型 -->
              <span class="absolute top-3 right-3 px-2 py-0.5 rounded text-xs bg-white/80 text-[var(--color-text-secondary)]">
                {{ ['','精装','毛坯','简装'][lp.decorateType] || '' }}
              </span>
            </div>
            <div class="p-5">
              <div class="flex items-start justify-between mb-2">
                <h3 class="text-base font-bold text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)] transition-colors line-clamp-1">
                  {{ lp.projectName }}
                </h3>
                <span v-if="lp.avgUnitPrice" class="text-sm font-bold text-[var(--color-danger)] whitespace-nowrap ml-2">
                  {{ lp.avgUnitPrice }}元/㎡
                </span>
              </div>
              <p class="text-xs text-[var(--color-text-tertiary)] mb-3">
                <MapPin class="w-3 h-3 inline -mt-0.5 mr-0.5" />{{ lp.district }}{{ lp.plate ? '·'+lp.plate : '' }}
              </p>
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
              <div class="text-xs text-[var(--color-text-tertiary)] truncate">
                {{ lp.projectCompany }}
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
            @change="fetchData"
          />
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section class="py-16 bg-gradient-to-r from-[#0052D9] to-[#366EF4]">
      <div class="section-container text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-white mb-4">没找到合适的楼盘？</h2>
        <p class="text-white/80 mb-8 text-sm max-w-md mx-auto">
          关注我们，第一时间获取新开盘、价格变动等最新消息
        </p>
        <t-button theme="default" size="large" variant="outline" class="!border-white !text-white !rounded-xl hover:!bg-white hover:!text-[#0052D9]" @click="searchKeyword=''; fetchData()">
          浏览全部楼盘
        </t-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Building2, MapPin } from 'lucide-vue-next'
import request from '@/utils/request'

const loupanList = ref([])
const loading = ref(false)
const total = ref(0)
const searchKeyword = ref('')
const filterDistrict = ref('')
const filterPlate = ref('')
const filterHouseType = ref(null)
const filterDecorate = ref(null)
const districtOpts = ref([])
const plateOpts = ref([])

const pg = reactive({ current: 1, pageSize: 12 })

const hotTags = ['钱塘区', '金沙湖', '建杭', '滨江']

const houseTypeOpts = [
  { label: '住宅', value: 1 }, { label: '公寓', value: 2 }, { label: '商铺', value: 3 }, { label: '别墅', value: 4 }
]
const decorateOpts = [
  { label: '精装', value: 1 }, { label: '毛坯', value: 2 }, { label: '简装', value: 3 }
]

async function fetchData() {
  loading.value = true
  try {
    const p = { page: pg.current, size: pg.pageSize }
    if (searchKeyword.value) p.keyword = searchKeyword.value
    if (filterDistrict.value) p.district = filterDistrict.value
    if (filterPlate.value) p.plate = filterPlate.value
    if (filterHouseType.value) p.houseType = filterHouseType.value
    if (filterDecorate.value) p.decorateType = filterDecorate.value
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

function doSearch() {
  pg.current = 1
  fetchData()
}

onMounted(() => { fetchData(); fetchFilters() })
</script>
