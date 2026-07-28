<template>
  <div class="ershoufang-page">
    <!-- 头部搜索区 -->
    <section class="relative overflow-hidden bg-gradient-to-br from-[#FFF7ED] via-white to-[#FEF2F2] py-10 lg:py-14">
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div class="absolute -top-40 -right-40 w-96 h-96 bg-orange-500/5 rounded-full blur-3xl" />
        <div class="absolute -bottom-40 -left-40 w-80 h-80 bg-red-500/5 rounded-full blur-3xl" />
      </div>
      <div class="section-container relative z-10 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-[var(--color-text-primary)] leading-tight mb-4">
          <span class="bg-gradient-to-r from-orange-500 to-red-500 bg-clip-text text-transparent">二手房</span>
        </h1>
        <p class="text-sm sm:text-base text-[var(--color-text-tertiary)] max-w-xl mx-auto mb-6 leading-relaxed">
          覆盖杭州全域二手房源，售罄/交付楼盘信息一览
        </p>
        <div class="max-w-2xl mx-auto bg-white rounded-2xl shadow-lg border border-gray-100 p-3 flex flex-col sm:flex-row gap-3">
          <t-input v-model="searchKeyword" placeholder="搜索楼盘名称、开发商、品牌..." size="large" clearable class="flex-1" @enter="doSearch" @clear="doSearch">
            <template #prefix-icon><Search class="w-5 h-5 text-[var(--color-text-tertiary)]" /></template>
          </t-input>
          <t-button theme="primary" size="large" class="!px-8 !rounded-xl !bg-orange-500 !border-orange-500 hover:!bg-orange-600" @click="doSearch">
            <Search class="w-4 h-4 mr-1.5" />搜索二手房
          </t-button>
        </div>
      </div>
    </section>

    <!-- 筛选栏 -->
    <section class="py-3 border-b border-gray-50 bg-white sticky top-[var(--header-height)] z-40">
      <div class="section-container flex flex-wrap items-center gap-2">
        <span class="text-xs text-[var(--color-text-secondary)] font-medium whitespace-nowrap">筛选条件</span>
        <t-select v-model="filterDistrict" placeholder="行政区" clearable size="small" class="w-[120px]" :options="districtOpts" @change="doSearch" />
        <t-select v-model="filterPlate" placeholder="板块" clearable size="small" class="w-[120px]" :options="plateOpts" @change="doSearch" />
        <t-select v-model="filterHouseType" placeholder="楼盘类型" clearable size="small" class="w-[110px]" :options="houseTypeOpts" @change="doSearch" />
        <t-select v-model="filterDecorate" placeholder="装修" clearable size="small" class="w-[100px]" :options="decorateOpts" @change="doSearch" />
        <span class="text-xs text-[var(--color-text-tertiary)] ml-auto">共 {{ total }} 套房源</span>
      </div>
    </section>

    <!-- 楼盘列表（每行一个楼盘 + 户型展示） -->
    <section class="py-8 bg-[#F8FAFE] min-h-[50vh]">
      <div class="section-container">
        <div v-if="loading" class="flex justify-center py-20"><t-loading size="large" text="加载中..." /></div>
        <div v-else-if="!loupanList.length" class="text-center py-20">
          <Building2 class="w-16 h-16 text-gray-200 mx-auto mb-4" />
          <p class="text-[var(--color-text-tertiary)]">暂无符合条件的二手房源</p>
        </div>
        <div v-else class="space-y-5">
          <div v-for="lp in loupanList" :key="lp.id" class="bg-white rounded-2xl border border-gray-100 overflow-hidden hover:shadow-md transition-all">
            <!-- 楼盘信息行 -->
            <div class="flex flex-col lg:flex-row">
              <!-- 左侧封面图 -->
              <div class="lg:w-72 h-48 lg:h-auto flex-shrink-0 bg-gradient-to-br from-orange-50 to-red-50 relative overflow-hidden">
                <t-image v-if="lp.coverImage" :src="lp.coverImage" fit="cover" class="w-full h-full absolute inset-0" />
                <Building2 v-else class="w-16 h-16 text-orange-300 absolute inset-0 m-auto" />
                <span :class="['absolute top-3 left-3 px-2.5 py-1 rounded-full text-xs font-medium',
                  lp.salesStatus===2?'bg-orange-500 text-white':
                  lp.salesStatus===3?'bg-blue-500 text-white':
                  'bg-green-500 text-white']">
                  {{ ['待售','在售','售罄','已交付'][lp.salesStatus] || '未知' }}
                </span>
              </div>
              <!-- 右侧楼盘详情 -->
              <div class="flex-1 p-5 flex flex-col justify-between min-w-0">
                <div>
                  <router-link :to="`/loupan/${lp.encodedId}`" class="block">
                    <h3 class="text-lg font-bold text-[var(--color-text-primary)] hover:text-orange-500 transition-colors mb-2">{{ lp.projectName }}</h3>
                  </router-link>
                  <p class="text-xs text-[var(--color-text-tertiary)] mb-2">
                    <MapPin class="w-3 h-3 inline -mt-0.5 mr-0.5" />{{ lp.district }}{{ lp.plate ? '·'+lp.plate : '' }}
                    <span class="mx-1.5 text-gray-200">|</span>
                    {{ ['','住宅','公寓','商铺','别墅'][lp.houseType] || '' }}
                    <span class="mx-1.5 text-gray-200">|</span>
                    {{ ['','精装','毛坯','简装'][lp.decorateType] || '' }}
                    <span class="mx-1.5 text-gray-200">|</span>
                    建面{{ lp.areaMin }}-{{ lp.areaMax }}㎡
                    <span v-if="lp.buildingTotal" class="mx-1.5 text-gray-200">|</span>
                    <span v-if="lp.buildingTotal">{{ lp.buildingTotal }}栋</span>
                  </p>
                  <p class="text-xs text-[var(--color-text-tertiary)] mb-3 truncate">{{ lp.projectCompany }}</p>
                  <!-- 标签 -->
                  <div v-if="lp.priceTag" class="flex flex-wrap gap-1.5 mb-2">
                    <span v-for="tag in lp.priceTag.split(',')" :key="tag" class="px-2 py-0.5 rounded text-xs bg-orange-50 text-orange-600 border border-orange-100">{{ tag }}</span>
                  </div>
                  <!-- 价格 -->
                  <div v-if="lp.avgUnitPrice" class="mb-3">
                    <span class="text-xl font-bold text-[var(--color-danger)]">{{ lp.avgUnitPrice }}</span>
                    <span class="text-xs text-[var(--color-text-tertiary)] ml-1">元/㎡（开盘均价）</span>
                  </div>
                </div>
              </div>
            </div>
            <!-- 户型横滑区域 -->
            <div v-if="lp.huxings && lp.huxings.length" class="border-t border-gray-50 px-5 py-3">
              <p class="text-xs text-[var(--color-text-tertiary)] mb-2 font-medium">户型</p>
              <div class="flex gap-3 overflow-x-auto pb-1">
                <router-link v-for="hx in lp.huxings" :key="hx.id" :to="`/loupan/${lp.encodedId}`" class="flex-shrink-0 w-40 border border-gray-100 rounded-lg overflow-hidden hover:border-orange-300 transition-colors group/hx">
                  <div class="aspect-[4/3] bg-gray-50 relative">
                    <t-image v-if="hx.huxingImage" :src="hx.huxingImage" fit="cover" class="w-full h-full" />
                    <span v-else class="absolute inset-0 flex items-center justify-center text-gray-300 text-xs">暂无图</span>
                  </div>
                  <div class="p-2">
                    <p class="text-xs font-bold text-[var(--color-text-primary)] group-hover/hx:text-orange-500 transition-colors truncate">{{ hx.huxingName }}</p>
                    <p class="text-xs text-[var(--color-text-secondary)]">{{ hx.area }}㎡ {{ hx.roomNum }}室{{ hx.hallNum }}厅</p>
                  </div>
                </router-link>
              </div>
            </div>
          </div>
        </div>
        <div v-if="total > pg.pageSize" class="flex justify-center mt-10">
          <t-pagination v-model:current="pg.current" :total="total" :page-size="pg.pageSize" size="medium" @change="fetchData" />
        </div>
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

const pg = reactive({ current: 1, pageSize: 10 })

const houseTypeOpts = [
  { label: '住宅', value: 1 }, { label: '公寓', value: 2 }, { label: '商铺', value: 3 }, { label: '别墅', value: 4 }
]
const decorateOpts = [
  { label: '精装', value: 1 }, { label: '毛坯', value: 2 }, { label: '简装', value: 3 }
]

async function fetchData() {
  loading.value = true
  try {
    const p = { page: pg.current, size: pg.pageSize, salesStatus: '2,3' }
    if (searchKeyword.value) p.keyword = searchKeyword.value
    if (filterDistrict.value) p.district = filterDistrict.value
    if (filterPlate.value) p.plate = filterPlate.value
    if (filterHouseType.value) p.houseType = filterHouseType.value
    if (filterDecorate.value) p.decorateType = filterDecorate.value
    const r = await request.get('/public/loupans', { params: p })
    loupanList.value = (r?.records || []).map(lp => ({ ...lp, huxings: [] }))
    total.value = r?.total || 0
    // 批量加载户型
    await loadBatchHuxings()
  } catch {} finally { loading.value = false }
}

async function loadBatchHuxings() {
  const ids = loupanList.value.filter(lp => lp.id).map(lp => lp.id)
  if (!ids.length) return
  try {
    const r = await request.get('/public/loupans/huxings/batch', { params: { loupanIds: ids.join(',') } })
    if (r) {
      loupanList.value.forEach(lp => {
        const list = r[lp.id] || []
        lp.huxings = list.slice(0, 6)
      })
    }
  } catch {}
}

async function fetchFilters() {
  try {
    const r = await request.get('/public/loupan-filters')
    if (r?.districts) districtOpts.value = r.districts.map(d => ({ label: d, value: d }))
    if (r?.plates) plateOpts.value = r.plates.map(p => ({ label: p, value: p }))
  } catch {}
}

function doSearch() { pg.current = 1; fetchData() }

onMounted(() => { fetchData(); fetchFilters() })
</script>
