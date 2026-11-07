<template>
  <div class="xinpan-page pb-6">
    <!-- 标题 + 筛选 -->
    <section class="bg-white border-b border-gray-50">
      <div class="section-container py-5">
        <div class="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h1 class="text-xl sm:text-2xl font-bold text-[var(--color-text-primary)] flex items-center gap-2">
              <Table2 class="w-6 h-6 text-[var(--color-primary)]" />
              新盘信息
            </h1>
            <p class="text-sm text-[var(--color-text-tertiary)] mt-1">杭州新盘关键信息一览，共 {{ total }} 个楼盘</p>
          </div>

          <div class="flex flex-wrap items-center gap-2 w-full sm:w-auto">
            <t-select v-model="district" :options="districtOptions" placeholder="全部区域" clearable
                      class="!w-32" @change="onFilterChange" />
            <t-input v-model="keyword" placeholder="搜索楼盘 / 品牌" clearable class="!w-48"
                     @enter="onFilterChange" @clear="onFilterChange">
              <template #prefix-icon><Search class="w-4 h-4" /></template>
            </t-input>
            <t-button variant="outline" @click="onFilterChange">查询</t-button>
          </div>
        </div>
      </div>
    </section>

    <section class="section-container pt-5">
      <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
        <div v-if="loading" class="py-16 text-center"><t-loading /></div>

        <div v-else-if="!list.length" class="py-16 text-center">
          <p class="text-sm text-[var(--color-text-tertiary)]">暂无符合条件的楼盘</p>
          <t-button v-if="keyword || district" variant="text" theme="primary" class="!mt-2" @click="resetFilter">清空筛选</t-button>
        </div>

        <template v-else>
          <div class="hidden sm:block overflow-x-auto">
            <table class="xinpan-table">
              <thead>
                <tr>
                  <th class="w-[80px]">区域</th>
                  <th class="w-[170px]">楼盘名称</th>
                  <th class="w-[90px]">板块</th>
                  <th class="w-[140px]">开发商品牌</th>
                  <th class="w-[100px]">面积段</th>
                  <th class="w-[120px]">单价</th>
                  <th class="w-[110px]">总价范围</th>
                  <th class="w-[130px]">物业费</th>
                  <th class="min-w-[220px]">优点</th>
                  <th class="min-w-[220px]">缺点</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="lp in list" :key="lp.id">
                  <td>{{ lp.district || '-' }}</td>
                  <td>
                    <router-link :to="`/loupan/${lp.encodedId}`" class="name-link">
                      {{ lp.projectName }}
                    </router-link>
                  </td>
                  <td>{{ lp.plate || '-' }}</td>
                  <td class="text-[13px]">{{ lp.brandList || '-' }}</td>
                  <td>{{ areaText(lp) }}</td>
                  <td class="price">{{ unitPriceText(lp) }}</td>
                  <td class="price">{{ totalPriceText(lp) }}</td>
                  <td class="text-[13px]">{{ feeText(lp) }}</td>
                  <td class="align-top">
                    <p class="cell-text" :title="lp.advantage">{{ lp.advantage || '-' }}</p>
                  </td>
                  <td class="align-top">
                    <p class="cell-text" :title="lp.disadvantage">{{ lp.disadvantage || '-' }}</p>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <p class="hidden sm:block lg:hidden text-center text-xs text-[var(--color-text-tertiary)] py-2">← 左右滑动查看完整表格 →</p>

          <!-- 移动端：表格列太多，改为卡片展示 -->
          <div class="sm:hidden divide-y divide-gray-50">
            <div v-for="lp in list" :key="lp.id" class="p-4">
              <div class="flex items-start justify-between gap-2">
                <router-link :to="`/loupan/${lp.encodedId}`" class="name-link text-[15px]">
                  {{ lp.projectName }}
                </router-link>
                <span class="text-xs text-[var(--color-text-tertiary)] shrink-0 pt-0.5">
                  {{ lp.district }}{{ lp.plate ? ' · ' + lp.plate : '' }}
                </span>
              </div>
              <div class="grid grid-cols-2 gap-x-3 gap-y-1 mt-2 text-[13px] text-[var(--color-text-primary)]">
                <p><span class="text-[var(--color-text-tertiary)]">开发商品牌 </span>{{ lp.brandList || '-' }}</p>
                <p><span class="text-[var(--color-text-tertiary)]">面积段 </span>{{ areaText(lp) }}</p>
                <p><span class="text-[var(--color-text-tertiary)]">单价 </span><span class="price-inline">{{ unitPriceText(lp) }}</span></p>
                <p><span class="text-[var(--color-text-tertiary)]">总价 </span><span class="price-inline">{{ totalPriceText(lp) }}</span></p>
                <p class="col-span-2"><span class="text-[var(--color-text-tertiary)]">物业费 </span>{{ feeText(lp) }}</p>
              </div>
              <p v-if="lp.advantage" class="mt-2 text-[13px] leading-5 text-[var(--color-text-secondary)]">
                <span class="text-emerald-600 font-medium">优点 </span>{{ lp.advantage }}
              </p>
              <p v-if="lp.disadvantage" class="mt-1 text-[13px] leading-5 text-[var(--color-text-secondary)]">
                <span class="text-rose-500 font-medium">缺点 </span>{{ lp.disadvantage }}
              </p>
            </div>
          </div>

          <div class="flex justify-center sm:justify-end px-4 py-4 border-t border-gray-50">
            <t-pagination
              :current="page"
              :total="total"
              :page-size="size"
              :page-size-options="[20, 50, 100]"
              :show-page-size="true"
              @current-change="onPageChange"
              @page-size-change="onSizeChange" />
          </div>
        </template>
      </div>

      <p class="text-xs text-[var(--color-text-tertiary)] mt-3 leading-5">
        数据来源于公开信息整理，仅供参考；实际价格、物业费以售楼处公示及网签合同为准。
      </p>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Table2, Search } from 'lucide-vue-next'
import request from '@/utils/request'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const keyword = ref('')
const district = ref('')
const districts = ref([])

const districtOptions = computed(() => districts.value.map(d => ({ label: d, value: d })))

async function fetchFilters() {
  try {
    const r = await request.get('/public/loupan-filters')
    districts.value = r?.districts || []
  } catch {
    districts.value = []
  }
}

async function fetchList() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    if (district.value) params.district = district.value
    const r = await request.get('/public/loupans', { params })
    list.value = r?.records || []
    total.value = r?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  page.value = 1
  fetchList()
}

function resetFilter() {
  keyword.value = ''
  district.value = ''
  onFilterChange()
}

function onPageChange(p) {
  page.value = p
  fetchList()
}

function onSizeChange(s) {
  size.value = s
  page.value = 1
  fetchList()
}

// ===== 单元格格式化 =====
function areaText(lp) {
  const a = Number(lp.areaMin || 0)
  const b = Number(lp.areaMax || 0)
  if (!a && !b) return '-'
  if (!b || a === b) return `${a || b}㎡`
  return `${a}-${b}㎡`
}

/** 高层均价为空时，回退到洋房/叠墅/排屋均价（与首页口径一致） */
function unitPriceText(lp) {
  const p = lp.avgUnitPrice || lp.avgUnitPriceYangfang || lp.avgUnitPriceDieshu || lp.avgUnitPricePaiwu
  return p ? `${Number(p).toLocaleString()}元/㎡` : '-'
}

function totalPriceText(lp) {
  const a = Number(lp.minTotalPrice || 0)
  const b = Number(lp.maxTotalPrice || 0)
  if (!a && !b) return '-'
  if (!b || a === b) return `${a || b}万`
  return `${a}-${b}万`
}

function feeText(lp) {
  const h = Number(lp.propertyFeeHigh || 0)
  const v = Number(lp.propertyFeeVilla || 0)
  if (h && v && h !== v) return `${h}-${v}元/㎡·月`
  const one = h || v
  return one ? `${one}元/㎡·月` : '-'
}

onMounted(() => {
  fetchFilters()
  fetchList()
})
</script>

<style scoped>
.xinpan-table {
  width: 100%;
  min-width: 1400px;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
  color: var(--color-text-primary);
}
.xinpan-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 12px 14px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
  background: #f5f7fb;
  border-bottom: 1px solid #e9edf3;
  white-space: nowrap;
}
.xinpan-table tbody td {
  padding: 12px 14px;
  border-bottom: 1px solid #f2f4f7;
  vertical-align: middle;
  line-height: 1.5;
}
.xinpan-table tbody tr:hover td {
  background: #f7faff;
}
.xinpan-table tbody tr:last-child td {
  border-bottom: 0;
}
.name-link {
  color: var(--color-primary);
  font-weight: 600;
  text-decoration: none;
}
.name-link:hover {
  text-decoration: underline;
}
.price,
.price-inline {
  color: #e34d59;
  font-weight: 600;
  white-space: nowrap;
}
.cell-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}
</style>
