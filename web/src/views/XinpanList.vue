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
            <p class="text-sm text-[var(--color-text-tertiary)] mt-1">杭州新盘关键信息一览，共 {{ total }} 个{{ salesStatusLabel }}楼盘</p>
          </div>

          <div class="flex flex-wrap items-center gap-2 w-full sm:w-auto">
            <t-select v-model="district" :options="districtOptions" placeholder="全部区域" clearable
                      class="!w-32" @change="onFilterChange" />
            <t-select v-model="salesStatus" :options="salesStatusOptions" placeholder="全部状态" clearable
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
          <t-button v-if="keyword || district || salesStatus !== '1'" variant="text" theme="primary" class="!mt-2"
                    @click="resetFilter">清空筛选</t-button>
        </div>

        <template v-else>
          <div class="hidden sm:block overflow-x-auto">
            <table class="xinpan-table">
              <colgroup>
                <col class="c-district" />
                <col class="c-name" />
                <col class="c-plate" />
                <col class="c-brand" />
                <col class="c-area" />
                <col class="c-price" />
                <col class="c-total" />
                <col class="c-fee" />
                <col />
                <col />
              </colgroup>
              <thead>
                <tr>
                  <th>区域</th>
                  <th>楼盘名称</th>
                  <th>板块</th>
                  <th>开发商品牌</th>
                  <th>面积段</th>
                  <th>单价</th>
                  <th>总价范围</th>
                  <th>物业费</th>
                  <th>优点</th>
                  <th>缺点</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="lp in list" :key="lp.id">
                  <td class="whitespace-nowrap">{{ lp.district || '-' }}</td>
                  <td>
                    <router-link :to="`/loupan/${lp.encodedId}`" class="name-link block truncate"
                                 :title="lp.projectName">
                      {{ lp.projectName }}
                    </router-link>
                  </td>
                  <td class="truncate" :title="lp.plate || ''">{{ lp.plate || '-' }}</td>
                  <td>
                    <p class="cell-brand" :title="lp.brandList || ''">{{ lp.brandList || '-' }}</p>
                  </td>
                  <td class="whitespace-nowrap">{{ areaText(lp) }}</td>
                  <td class="price">{{ unitPriceText(lp) }}</td>
                  <td class="price">{{ totalPriceText(lp) }}</td>
                  <td class="text-[13px] whitespace-nowrap">{{ feeText(lp) }}</td>
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
const salesStatus = ref('1')  // 默认只看在售楼盘
const districts = ref([])

const districtOptions = computed(() => districts.value.map(d => ({ label: d, value: d })))

// 销售状态：0 待售 / 1 在售 / 2 售罄 / 3 已交付（与 Loupan.salesStatus 一致）
const salesStatusOptions = [
  { label: '在售', value: '1' },
  { label: '待售', value: '0' },
  { label: '售罄', value: '2' },
  { label: '已交付', value: '3' },
]

/** 副标题用：当前状态下显示「共 N 个在售楼盘」 */
const salesStatusLabel = computed(() =>
  salesStatusOptions.find(o => o.value === salesStatus.value)?.label || ''
)

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
    // orderByDistrict：按杭州行政区官方顺序分组展示
    const params = { page: page.value, size: size.value, orderByDistrict: true }
    if (keyword.value) params.keyword = keyword.value
    if (district.value) params.district = district.value
    if (salesStatus.value !== '') params.salesStatus = salesStatus.value
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
  salesStatus.value = '1'
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
  table-layout: fixed;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 14px;
  color: var(--color-text-primary);
}
/* 列宽：8 个固定列 + 优点/缺点均分剩余空间，容器内不产生横向滚动 */
.xinpan-table .c-district { width: 70px; }
.xinpan-table .c-name { width: 176px; }
.xinpan-table .c-plate { width: 92px; }
.xinpan-table .c-brand { width: 118px; }
.xinpan-table .c-area { width: 94px; }
.xinpan-table .c-price { width: 106px; }
.xinpan-table .c-total { width: 110px; }
.xinpan-table .c-fee { width: 134px; }

.xinpan-table thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 12px 10px;
  text-align: left;
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
  background: #f5f7fb;
  border-bottom: 1px solid #e9edf3;
  white-space: nowrap;
}
.xinpan-table tbody td {
  padding: 12px 10px;
  border-bottom: 1px solid #f2f4f7;
  vertical-align: middle;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
}
.xinpan-table th:first-child,
.xinpan-table td:first-child {
  padding-left: 16px;
}
.xinpan-table th:last-child,
.xinpan-table td:last-child {
  padding-right: 16px;
}

/* 1024–1279：收紧列宽与字号，保证 PC 端仍无横向滚动 */
@media (min-width: 1024px) and (max-width: 1279px) {
  .xinpan-table {
    font-size: 13px;
  }
  .xinpan-table .c-district { width: 60px; }
  .xinpan-table .c-name { width: 140px; }
  .xinpan-table .c-plate { width: 86px; }
  .xinpan-table .c-brand { width: 100px; }
  .xinpan-table .c-area { width: 86px; }
  .xinpan-table .c-price { width: 100px; }
  .xinpan-table .c-total { width: 104px; }
  .xinpan-table .c-fee { width: 132px; }
  .xinpan-table thead th,
  .xinpan-table tbody td {
    padding-left: 8px;
    padding-right: 8px;
  }
  .xinpan-table th:first-child,
  .xinpan-table td:first-child {
    padding-left: 12px;
  }
  .xinpan-table th:last-child,
  .xinpan-table td:last-child {
    padding-right: 14px;
  }
}

/* 平板（<1024）：列太多，保留横向滚动 + 提示 */
@media (max-width: 1023px) {
  .xinpan-table {
    table-layout: auto;
    min-width: 980px;
  }
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
/* 品牌可能多个（如「滨江集团/绿城中国」），最多两行 */
.cell-brand {
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-all;
}
</style>
