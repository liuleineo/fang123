<template>
  <div class="loupan-sub-page">
    <!-- 面包屑 -->
    <div class="section-container py-4">
      <t-breadcrumb>
        <t-breadcrumb-item :to="`/loupan/${route.params.id}`">楼盘详情</t-breadcrumb-item>
        <t-breadcrumb-item>{{ title }}</t-breadcrumb-item>
      </t-breadcrumb>
    </div>

    <!-- 渲染错误提示（便于排查白屏/空页） -->
    <div v-if="renderErr" class="mx-auto max-w-[1200px] px-4 mb-2 bg-red-50 border border-red-200 text-red-600 text-xs p-3 rounded">
      页面渲染出错：{{ renderErr }}
    </div>

    <!-- 标题 -->
    <section class="bg-white border-b border-gray-50">
      <div class="section-container py-5">
        <h1 class="text-xl sm:text-2xl font-bold text-[var(--color-text-primary)] flex items-center gap-2">
          <component :is="titleIcon" class="w-6 h-6 text-[var(--color-primary)]" />
          {{ title }}
        </h1>
        <p v-if="loupan?.projectName" class="text-sm text-[var(--color-text-tertiary)] mt-1">{{ loupan.projectName }}</p>
      </div>
    </section>

    <section class="py-6 bg-[#F8FAFE] min-h-[60vh]">
      <div class="section-container">
        <!-- 图库 -->
        <template v-if="subType === 'media'">
          <div v-if="mediaLoading" class="text-center py-16"><t-loading /></div>
          <div v-else-if="!medias.length" class="text-center py-16 text-[var(--color-text-tertiary)]">暂无图片素材</div>
          <div v-else class="space-y-6">
            <div v-for="group in mediaGroups" :key="group.label">
              <h4 class="text-base font-bold text-[var(--color-text-primary)] mb-3">{{ group.label }}（{{ group.items.length }}）</h4>
              <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
                <div v-for="m in group.items" :key="m.id" class="aspect-[4/3] rounded-xl overflow-hidden" :class="m.mediaType===5?'bg-black':(m.mediaType===6?'bg-gray-100':'bg-gray-100 cursor-pointer hover:opacity-90 transition-opacity')" @click="previewMedia(group.items, m)">
                  <t-image v-if="m.mediaType!==5&&m.mediaType!==6" :src="m.mediaUrl" fit="cover" class="w-full h-full" />
                  <!-- 短视频：直接内嵌播放器 -->
                  <video v-else-if="m.mediaType===5" :src="m.mediaUrl" controls muted playsinline preload="metadata" class="w-full h-full object-cover" />
                  <div v-else class="w-full h-full flex items-center justify-center bg-gray-200 text-sm text-[var(--color-text-tertiary)]">
                    {{ m.mediaType===6?'VR全景':'短视频' }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 户型 -->
        <template v-else-if="subType === 'huxing'">
          <div v-if="huxingLoading" class="text-center py-16"><t-loading /></div>
          <div v-else-if="!huxings.length" class="text-center py-16 text-[var(--color-text-tertiary)]">暂无户型信息</div>
          <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            <div v-for="hx in huxings" :key="hx.id" class="border border-gray-100 rounded-xl overflow-hidden hover:shadow-md transition-all">
              <div class="p-5">
                <div class="flex items-start justify-between mb-3">
                  <h4 class="font-bold text-[var(--color-text-primary)]">{{ hx.huxingName }}</h4>
                  <span v-if="hx.isShowHouse" class="px-2 py-0.5 text-xs rounded bg-green-50 text-green-600 border border-green-100">有样板间</span>
                </div>
              </div>
              <div v-if="hx.huxingImage" class="aspect-[4/3] bg-gray-50 cursor-pointer hover:opacity-90 transition-opacity" @click="previewImages(hx.huxingImage, 0)">
                <t-image :src="hx.huxingImage" fit="cover" class="w-full h-full" />
              </div>
              <!-- 标准户型图 + 户型视频（如有） -->
              <div v-if="hxExtras(hx).length" class="px-5 py-3 flex items-center gap-3 bg-[#FAFBFD] border-t border-gray-100">
                <div v-for="(ex, i) in hxExtras(hx)" :key="i" class="flex flex-col items-center gap-1 cursor-pointer group" @click="previewHuxingExtra(hx, ex)">
                  <div class="relative w-20 h-20 rounded-lg overflow-hidden bg-gray-100 border border-gray-200 group-hover:opacity-80 transition-opacity">
                    <img v-if="ex.kind === 'image'" :src="ex.url" alt="" class="w-full h-full object-cover" />
                    <video v-else :src="ex.url" muted playsinline preload="metadata" class="w-full h-full object-cover" />
                    <div v-if="ex.kind === 'video'" class="absolute inset-0 flex items-center justify-center bg-black/30">
                      <Play class="w-6 h-6 text-white" />
                    </div>
                  </div>
                  <span class="text-xs text-[var(--color-text-tertiary)]">{{ ex.label }}</span>
                </div>
              </div>
              <div class="p-5 pt-0">
                <p class="text-2xl font-bold text-[var(--color-primary)] mb-1">{{ hx.area }}<span class="text-sm font-normal text-[var(--color-text-tertiary)]">㎡</span></p>
                <p class="text-sm text-[var(--color-text-secondary)] mb-3">{{ hx.roomNum }}室{{ hx.hallNum }}厅{{ hx.toiletNum }}卫
                  <span v-if="hx.balconyNum">· {{ hx.balconyNum }}阳台</span>
                </p>
                <div class="flex flex-wrap gap-1.5 text-xs">
                  <span class="px-2 py-0.5 rounded bg-blue-50 text-[var(--color-primary)]">{{ ['','高层','小高层','洋房','叠墅','排屋'][hx.floorType] }}</span>
                  <span v-if="hx.unitPrice" class="px-2 py-0.5 rounded bg-gray-50 text-[var(--color-text-secondary)]">{{ hx.unitPrice }}元/㎡</span>
                  <span v-if="hx.totalPriceStart" class="px-2 py-0.5 rounded bg-orange-50 text-orange-600">{{ hx.totalPriceStart }}-{{ hx.totalPriceEnd }}万</span>
                </div>
                <div v-if="hx.tag" class="mt-3 flex flex-wrap gap-1">
                  <span v-for="t in hx.tag.split(',')" :key="t" class="px-2 py-0.5 text-xs rounded-full bg-gray-50 text-[var(--color-text-tertiary)]">{{ t }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 一房一价 -->
        <template v-else-if="subType === 'yfyj'">
          <div v-if="yfyjLoading" class="text-center py-16"><t-loading /></div>
          <div v-else-if="!yfyjList.length" class="text-center py-16 text-[var(--color-text-tertiary)]">暂无一房一价信息</div>
          <div v-else>
            <!-- 楼栋切换 -->
            <div class="flex flex-wrap gap-2 mb-4">
              <t-button v-for="bd in yfyjBuildings" :key="bd" :theme="yfyjBuilding===bd?'primary':'default'" :variant="yfyjBuilding===bd?'base':'outline'" size="small" @click="yfyjBuilding=bd">{{ bd }}号楼</t-button>
            </div>
            <!-- 多个单元并排展示 -->
            <div class="overflow-x-auto bg-white rounded-lg border border-gray-100">
              <table class="w-full text-sm border-collapse table-fixed">
                <thead>
                  <tr class="bg-gray-50">
                    <th class="p-2 font-medium text-center text-[var(--color-text-secondary)] w-12">楼层</th>
                    <template v-for="unit in yfyjUnits" :key="unit">
                      <th v-for="pos in yfyjUnitPositions(unit)" :key="unit+'-'+pos" class="p-2 font-medium text-center text-[var(--color-text-secondary)] border-l border-gray-200">
                        <div class="text-xs text-[var(--color-text-tertiary)]">{{ unit }}单元</div>
                        <div>{{ pos }}室</div>
                      </th>
                    </template>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="floor in yfyjAllFloors" :key="floor" class="border-t border-gray-100">
                    <td class="p-1 text-center font-bold text-xs text-[var(--color-text-tertiary)] bg-gray-50 align-middle">{{ floor }}F</td>
                    <template v-for="unit in yfyjUnits" :key="unit">
                      <td v-for="pos in yfyjUnitPositions(unit)" :key="unit+'-'+pos" class="p-1 border-l border-gray-100">
                        <div v-if="yfyjUnitGrid(unit)[floor]?.[pos]" class="rounded p-2 text-center text-xs min-h-[64px] flex flex-col justify-center"
                             :class="{
                               'bg-gray-50 text-gray-500': yfyjUnitGrid(unit)[floor][pos].houseStatus===0,
                               'bg-orange-50 text-orange-700': yfyjUnitGrid(unit)[floor][pos].houseStatus===1,
                               'bg-red-50 text-red-700': yfyjUnitGrid(unit)[floor][pos].houseStatus===2,
                               'bg-purple-50 text-purple-700': yfyjUnitGrid(unit)[floor][pos].houseStatus===3,
                               'bg-blue-50 text-blue-700': yfyjUnitGrid(unit)[floor][pos].houseStatus===4
                             }">
                          <div class="font-bold">{{ yfyjUnitGrid(unit)[floor][pos].roomNo }}</div>
                          <div class="opacity-80">{{ yfyjUnitGrid(unit)[floor][pos].area }}㎡</div>
                          <div class="opacity-80" v-if="yfyjUnitGrid(unit)[floor][pos].recordUnitPrice">{{ yfyjUnitGrid(unit)[floor][pos].recordUnitPrice }}元/㎡</div>
                          <div class="opacity-80" v-if="yfyjUnitGrid(unit)[floor][pos].recordTotalPrice">{{ (yfyjUnitGrid(unit)[floor][pos].recordTotalPrice/10000).toFixed(2) }}万</div>
                          <div class="font-medium mt-0.5">{{ ['未售','认购','已售','抵押','保留'][yfyjUnitGrid(unit)[floor][pos].houseStatus] }}</div>
                        </div>
                        <div v-else class="rounded p-1.5 text-center text-xs text-gray-300 bg-gray-50">-</div>
                      </td>
                    </template>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>

        <!-- 楼盘动态 -->
        <template v-else-if="subType === 'dynamic'">
          <div v-if="dynamicLoading" class="text-center py-16"><t-loading /></div>
          <div v-else-if="!dynamics.length" class="text-center py-16 text-[var(--color-text-tertiary)]">暂无楼盘动态</div>
          <div v-else class="space-y-4">
            <div v-for="d in dynamics" :key="d.id" class="bg-white rounded-xl border border-gray-100 p-5">
              <div class="flex items-center gap-2 mb-2">
                <t-tag size="small" :theme="typeTheme[d.type]||'default'">{{ typeMap[d.type]||'动态' }}</t-tag>
                <span class="text-sm font-bold text-[var(--color-text-primary)]">{{ d.title }}</span>
              </div>
              <div v-if="d.images" class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2 mb-3">
                <img v-for="(url,i) in (d.images||'').split(',').filter(Boolean)" :key="i" :src="url" class="w-full aspect-[4/3] object-cover rounded-lg border border-gray-100 cursor-pointer hover:opacity-90 transition-opacity" @error="e=>e.target.style.display='none'" @click="previewImages(d.images, i)" />
              </div>
              <p class="text-sm leading-6 text-[var(--color-text-secondary)] whitespace-pre-wrap">{{ d.content }}</p>
              <p class="text-xs text-[var(--color-text-tertiary)] mt-3">{{ fmt(d.createTime) }}</p>
            </div>
          </div>
        </template>

        <!-- 真实成交（表格） -->
        <template v-else-if="subType === 'real-deal'">
          <div v-if="realDealLoading" class="text-center py-16"><t-loading /></div>
          <div v-else-if="!realDeals.length" class="text-center py-16 text-[var(--color-text-tertiary)]">暂无真实成交信息</div>
          <div v-else class="overflow-x-auto bg-white rounded-xl border border-gray-100">
            <table class="w-full text-sm border-collapse">
              <thead>
                <tr class="bg-gray-50 text-left text-[var(--color-text-secondary)]">
                  <th class="p-3 font-medium whitespace-nowrap">成交时间</th>
                  <th class="p-3 font-medium whitespace-nowrap">房号</th>
                  <th class="p-3 font-medium whitespace-nowrap text-right">面积(㎡)</th>
                  <th class="p-3 font-medium whitespace-nowrap text-right">成交单价</th>
                  <th class="p-3 font-medium whitespace-nowrap text-right">成交总价(万)</th>
                  <th class="p-3 font-medium whitespace-nowrap">备注</th>
                  <th class="p-3 font-medium whitespace-nowrap text-right">一手买入价(万)</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in realDeals" :key="r.id" class="border-t border-gray-100 hover:bg-gray-50">
                  <td class="p-3 text-[var(--color-text-secondary)] whitespace-nowrap">{{ r.dealDate||'-' }}</td>
                  <td class="p-3 font-medium text-[var(--color-text-primary)] whitespace-nowrap">{{ r.roomNo||'-' }}</td>
                  <td class="p-3 text-right text-[var(--color-text-secondary)] whitespace-nowrap">{{ r.houseArea!=null?Number(r.houseArea):'-' }}</td>
                  <td class="p-3 text-right text-[var(--color-text-secondary)] whitespace-nowrap">{{ (r.dealPrice!=null&&r.houseArea) ? (Number(r.dealPrice)*10000/Number(r.houseArea)).toFixed(0)+'元/㎡' : '-' }}</td>
                  <td class="p-3 text-right font-bold text-[var(--color-danger)] whitespace-nowrap">{{ r.dealPrice!=null?Number(r.dealPrice):'-' }}</td>
                  <td class="p-3 text-[var(--color-text-secondary)] max-w-[200px] truncate">{{ r.remark||'-' }}</td>
                  <td class="p-3 text-right text-[var(--color-primary)] whitespace-nowrap">{{ r.yfyj!=null?Number(r.yfyj):'-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 成交均价走势：X 月 / Y 成交平均单价 -->
          <div class="mt-6 bg-white rounded-xl border border-gray-100 p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-base font-bold text-[var(--color-text-primary)]">成交均价走势</h3>
              <span class="text-xs text-[var(--color-text-tertiary)]">按月平均单价（元/㎡）</span>
            </div>
            <!-- 无任何可统计月份 -->
            <div v-if="priceTrend.mode === 'none'" class="py-10 text-center text-sm text-[var(--color-text-tertiary)]">暂无足够数据绘制走势图</div>
            <!-- 仅 1 个月数据：展示当月均价统计，不画折线 -->
            <div v-else-if="priceTrend.mode === 'single'" class="py-5 flex flex-col items-center">
              <p class="text-xs text-[var(--color-text-tertiary)] mb-2">{{ priceTrend.single.month }} 月成交 {{ priceTrend.single.count }} 套，均价</p>
              <p class="text-3xl font-bold text-[var(--color-primary)]">{{ priceTrend.single.avg.toLocaleString() }}<span class="text-sm font-normal text-[var(--color-text-tertiary)] ml-1">元/㎡</span></p>
              <p class="text-xs text-[var(--color-text-tertiary)] mt-3">当前仅 1 个月成交数据，累计更多月份后自动展示走势曲线</p>
            </div>
            <!-- 2 个月及以上：折线图 -->
            <div v-else class="overflow-x-auto">
              <svg :viewBox="`0 0 ${CHART_W} ${CHART_H}`" width="720" class="block min-w-[560px] md:min-w-0">
                <!-- 横向网格线与 Y 轴刻度 -->
                <g v-for="g in priceTrend.grid" :key="g.val">
                  <line :x1="CHART_PL" :y1="g.y" :x2="CHART_W - CHART_PR" :y2="g.y" stroke="#EEF0F4" stroke-width="1" />
                  <text :x="CHART_PL - 8" :y="g.y + 3.5" text-anchor="end" font-size="11" fill="#9AA0A6">{{ g.val.toLocaleString() }}</text>
                </g>
                <!-- 面积填充 + 折线 -->
                <polygon :points="priceTrend.areaPoints" fill="#0052D9" opacity="0.05" />
                <polyline :points="priceTrend.linePoints" fill="none" stroke="#0052D9" stroke-width="2.5" stroke-linejoin="round" stroke-linecap="round" />
                <!-- 数据点（悬浮显示月份与均价） -->
                <g v-for="d in priceTrend.dots" :key="d.month">
                  <circle :cx="d.x" :cy="d.y" r="4" fill="#fff" stroke="#0052D9" stroke-width="2">
                    <title>{{ d.title }}</title>
                  </circle>
                </g>
                <!-- X 轴月份刻度 -->
                <g v-for="t in priceTrend.ticks" :key="t.x">
                  <text :x="t.x" :y="CHART_H - 12" text-anchor="middle" font-size="11" fill="#9AA0A6">{{ t.label }}</text>
                </g>
                <!-- 坐标轴 -->
                <line :x1="CHART_PL" :y1="CHART_H - CHART_PB + 6" :x2="CHART_W - CHART_PR" :y2="CHART_H - CHART_PB + 6" stroke="#E5E8EE" stroke-width="1" />
                <line :x1="CHART_PL" :y1="CHART_PT" :x2="CHART_PL" :y2="CHART_H - CHART_PB + 6" stroke="#E5E8EE" stroke-width="1" />
              </svg>
            </div>
          </div>

          <!-- 同板块真实成交（最近30条） -->
          <div v-if="plateLoaded" class="mt-8">
            <div v-if="plateName" class="flex items-center gap-2 mb-3">
              <h2 class="text-lg font-bold text-[var(--color-text-primary)]">同板块真实成交</h2>
              <span class="px-1.5 py-0.5 rounded text-xs bg-blue-50 text-[var(--color-primary)]">{{ plateName }}</span>
              <span v-if="plateDeals.length" class="text-xs text-[var(--color-text-tertiary)]">最近 {{ plateDeals.length }} 条</span>
            </div>
            <div v-if="plateName && !plateDeals.length" class="bg-white rounded-xl border border-gray-100 py-10 text-center text-[var(--color-text-tertiary)] text-sm">暂无同板块其他楼盘成交信息</div>
            <div v-else-if="plateDeals.length" class="overflow-x-auto bg-white rounded-xl border border-gray-100">
              <table class="w-full text-sm border-collapse">
                <thead>
                  <tr class="bg-gray-50 text-left text-[var(--color-text-secondary)]">
                    <th class="p-3 font-medium whitespace-nowrap">成交时间</th>
                    <th class="p-3 font-medium whitespace-nowrap">小区</th>
                    <th class="p-3 font-medium whitespace-nowrap">房号</th>
                    <th class="p-3 font-medium whitespace-nowrap text-right">面积(㎡)</th>
                    <th class="p-3 font-medium whitespace-nowrap text-right">成交单价</th>
                    <th class="p-3 font-medium whitespace-nowrap text-right">成交总价(万)</th>
                    <th class="p-3 font-medium whitespace-nowrap">备注</th>
                    <th class="p-3 font-medium whitespace-nowrap text-right">一手买入价(万)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="r in plateDeals" :key="r.id" class="border-t border-gray-100 hover:bg-gray-50">
                    <td class="p-3 text-[var(--color-text-secondary)] whitespace-nowrap">{{ r.dealDate||'-' }}</td>
                    <td class="p-3 font-medium text-[var(--color-text-primary)] whitespace-nowrap">{{ r.communityName||'-' }}</td>
                    <td class="p-3 text-[var(--color-text-secondary)] whitespace-nowrap">{{ r.roomNo||'-' }}</td>
                    <td class="p-3 text-right text-[var(--color-text-secondary)] whitespace-nowrap">{{ r.houseArea!=null?Number(r.houseArea):'-' }}</td>
                    <td class="p-3 text-right text-[var(--color-text-secondary)] whitespace-nowrap">{{ (r.dealPrice!=null&&r.houseArea) ? (Number(r.dealPrice)*10000/Number(r.houseArea)).toFixed(0)+'元/㎡' : '-' }}</td>
                    <td class="p-3 text-right font-bold text-[var(--color-danger)] whitespace-nowrap">{{ r.dealPrice!=null?Number(r.dealPrice):'-' }}</td>
                    <td class="p-3 text-[var(--color-text-secondary)] max-w-[200px] truncate">{{ r.remark||'-' }}</td>
                    <td class="p-3 text-right text-[var(--color-primary)] whitespace-nowrap">{{ r.yfyj!=null?Number(r.yfyj):'-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
      </div>
    </section>

    <!-- 图片全屏预览 -->
    <t-image-viewer v-model:visible="viewerVisible" :images="viewerImages" v-model:index="viewerIndex" />

    <!-- 户型视频放大播放 -->
    <t-dialog v-model:visible="huxingVideoVisible" header="户型视频" width="720px" :footer="false" :close-on-overlay-click="true">
      <video v-if="huxingVideoUrl" :src="huxingVideoUrl" controls autoplay playsinline class="w-full rounded-lg bg-black" style="max-height:70vh" />
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onErrorCaptured } from 'vue'
import { useRoute } from 'vue-router'
import { Images, LayoutGrid, BadgeCent, Newspaper, HandCoins, Play } from 'lucide-vue-next'
import request from '@/utils/request'

const route = useRoute()
// 渲染错误捕获：出错时页面显示错误信息而非白屏，便于定位
const renderErr = ref('')
onErrorCaptured(e => {
  renderErr.value = (e && e.message) ? e.message : String(e)
  console.error('LoupanSubPage 渲染错误:', e)
  return false
})
const loupan = ref(null)
const huxings = ref([])
const medias = ref([])
const yfyjList = ref([])
const dynamics = ref([])
const realDeals = ref([])
const huxingLoading = ref(false)
const mediaLoading = ref(false)
const yfyjLoading = ref(false)
const dynamicLoading = ref(false)
const realDealLoading = ref(false)
// 兜底：加载状态异常卡死时 10s 后强制关闭，避免页面一直转圈
let realDealLoadTimer = null
watch(realDealLoading, v => {
  clearTimeout(realDealLoadTimer)
  if (v) realDealLoadTimer = setTimeout(() => { realDealLoading.value = false }, 10000)
})
const plateDeals = ref([])
const plateName = ref('')
const plateLoaded = ref(false)
const yfyjBuilding = ref('')

const typeMap = { 1: '建设动态', 2: '销售动态', 3: '优惠动态' }
const typeTheme = { 1: 'primary', 2: 'success', 3: 'warning' }

// 根据路由 name 判断子页面类型
const subType = computed(() => {
  if (route.name === 'LoupanMedia') return 'media'
  if (route.name === 'LoupanHuxing') return 'huxing'
  if (route.name === 'LoupanYfyj') return 'yfyj'
  if (route.name === 'LoupanDynamic') return 'dynamic'
  if (route.name === 'LoupanRealDeal') return 'real-deal'
  return ''
})

const titleMap = {
  media: '楼盘图库',
  huxing: '楼盘户型',
  yfyj: '一房一价',
  dynamic: '楼盘动态',
  'real-deal': '真实成交'
}
const title = computed(() => titleMap[subType.value] || '')

const iconMap = {
  media: Images,
  huxing: LayoutGrid,
  yfyj: BadgeCent,
  dynamic: Newspaper,
  'real-deal': HandCoins
}
const titleIcon = computed(() => iconMap[subType.value] || Images)

const mediaGroups = computed(() => {
  const types = { 1: '实景图', 2: '样板间', 3: '户型图', 4: '航拍', 5: '短视频', 6: 'VR全景', 7: '设计图', 8: '区位图', 9: '效果图', 10: '施工进度', 11: '周边配套' }
  const map = {}
  medias.value.forEach(m => {
    const key = types[m.mediaType] || '其他'
    if (!map[key]) map[key] = []
    map[key].push(m)
  })
  return Object.entries(map).map(([label, items]) => ({ label, items }))
})

const yfyjBuildings = computed(() => [...new Set(yfyjList.value.map(i=>i.buildingNo))].sort((a,b)=>Number(a)-Number(b)))
const yfyjUnits = computed(() => [...new Set(yfyjList.value.filter(i=>i.buildingNo===yfyjBuilding.value).map(i=>i.unitNo))].filter(Boolean).sort())

const yfyjAllGrids = computed(() => {
  const all = {}
  yfyjUnits.value.forEach(unit => {
    const grid = {}
    yfyjList.value.filter(i=>i.buildingNo===yfyjBuilding.value && i.unitNo===unit).forEach(item => {
      const rn = String(item.roomNo)
      const floor = parseInt(rn.slice(0, -2)) || 0
      const suffix = rn.slice(-2)
      if (!grid[floor]) grid[floor] = {}
      grid[floor][suffix] = item
    })
    all[unit] = grid
  })
  return all
})

function yfyjUnitGrid(unit) { return yfyjAllGrids.value[unit] || {} }
function yfyjUnitPositions(unit) {
  const set = new Set()
  Object.values(yfyjUnitGrid(unit)).forEach(row => Object.keys(row).forEach(k => set.add(k)))
  return [...set].sort()
}
const yfyjAllFloors = computed(() => {
  const set = new Set()
  yfyjUnits.value.forEach(unit => Object.keys(yfyjUnitGrid(unit)).forEach(f => set.add(Number(f))))
  return [...set].sort((a,b)=>b-a)
})

const id = computed(() => route.params.id)

async function fetchDetail() {
  try {
    loupan.value = await request.get(`/public/loupans/${id.value}`)
  } catch {}
}

async function fetchHuxings() {
  huxingLoading.value = true
  try {
    huxings.value = await request.get(`/public/loupans/${id.value}/huxings`) || []
  } catch {} finally { huxingLoading.value = false }
}

async function fetchMedias() {
  mediaLoading.value = true
  try {
    medias.value = await request.get(`/public/loupans/${id.value}/medias`) || []
  } catch {} finally { mediaLoading.value = false }
}

async function fetchYfyj() {
  yfyjLoading.value = true
  try {
    yfyjList.value = await request.get(`/public/loupans/${id.value}/yfyj`) || []
    if (yfyjBuildings.value.length) yfyjBuilding.value = yfyjBuildings.value[0]
  } catch {} finally { yfyjLoading.value = false }
}

async function fetchDynamics() {
  dynamicLoading.value = true
  try {
    dynamics.value = await request.get(`/public/loupans/${id.value}/dynamics`) || []
  } catch {} finally { dynamicLoading.value = false }
}

async function fetchRealDeals() {
  realDealLoading.value = true
  try {
    realDeals.value = await request.get(`/public/loupans/${id.value}/real-deals`) || []
  } catch (e) {
    console.error('真实成交加载失败:', e)
  } finally { realDealLoading.value = false }
}

// ===== 成交均价走势（按月平均单价）=====
const CHART_W = 720
const CHART_H = 300
const CHART_PL = 56
const CHART_PR = 20
const CHART_PT = 22
const CHART_PB = 36

/** 按成交月份聚合：当月成交均价 = 当月总成交金额(元)之和 / 总面积(㎡)之和，升序排列 */
const monthAvg = computed(() => {
  const map = new Map()
  for (const r of realDeals.value) {
    if (r.dealPrice == null || !r.houseArea) continue
    const m = /^(\d{4})[-/年](\d{1,2})/.exec(String(r.dealDate || ''))
    if (!m) continue
    const key = `${m[1]}-${m[2].padStart(2, '0')}`
    const amount = Number(r.dealPrice) * 10000
    const area = Number(r.houseArea)
    const e = map.get(key) || { amount: 0, area: 0, count: 0 }
    e.amount += amount
    e.area += area
    e.count++
    map.set(key, e)
  }
  return [...map.entries()]
    .filter(([, e]) => e.area > 0)
    .map(([month, e]) => ({ month, avg: Math.round(e.amount / e.area), count: e.count }))
    .sort((a, b) => (a.month < b.month ? -1 : 1))
})

/**
 * 走势图统一状态源：mode = none(无数据) / single(仅1个月) / multi(≥2个月)
 * 模板只依据 priceTrend 一个 computed 分支，避免多 computed 状态撕裂导致渲染崩溃
 */
const priceTrend = computed(() => {
  try {
    const pts = monthAvg.value
    if (!pts.length) return { mode: 'none' }
    if (pts.length === 1) return { mode: 'single', single: pts[0] }
    const innerW = CHART_W - CHART_PL - CHART_PR
    const innerH = CHART_H - CHART_PT - CHART_PB
    const vs = pts.map(p => p.avg)
    let minV = Math.min(...vs)
    let maxV = Math.max(...vs)
    // 数值异常兜底，避免 NaN 污染坐标
    if (!isFinite(minV) || !isFinite(maxV)) { minV = 0; maxV = 1 }
    if (minV === maxV) {
      const pad = Math.max(Math.round(minV * 0.1), 1000)
      minV = Math.max(0, minV - pad)
      maxV += pad
    } else {
      const pad = (maxV - minV) * 0.12
      maxV += pad
      minV = Math.max(0, minV - pad)
    }
    const span = maxV - minV || 1
    const n = pts.length
    const xAt = i => CHART_PL + (i / (n - 1)) * innerW
    const yAt = v => CHART_PT + ((maxV - v) / span) * innerH
    const yBase = CHART_PT + innerH
    const line = pts.map((p, i) => ({ x: xAt(i), y: yAt(p.avg), month: p.month, avg: p.avg }))
    const dots = line.map(p => ({
      ...p,
      label: p.month.slice(2).replace('-', '/'),
      title: `${p.month} 月均价 ${p.avg.toLocaleString()} 元/㎡`
    }))
    const grid = Array.from({ length: 5 }, (_, k) => ({
      y: CHART_PT + innerH - (innerH * k) / 4,
      val: Math.round(minV + (span * k) / 4)
    }))
    const maxTicks = 6
    const tickIdxs = n <= maxTicks
      ? pts.map((_, i) => i)
      : [...new Set([0, ...[...Array(maxTicks - 2)].map((_, k) => Math.round(((n - 1) * (k + 1)) / (maxTicks - 1))), n - 1])]
    return {
      mode: 'multi',
      grid,
      dots,
      linePoints: line.map(p => `${p.x},${p.y}`).join(' '),
      areaPoints: `${line.map(p => `${p.x},${p.y}`).join(' ')} ${CHART_W - CHART_PR},${yBase} ${CHART_PL},${yBase}`,
      ticks: tickIdxs.map(i => ({ x: line[i].x, label: dots[i].label }))
    }
  } catch (e) {
    console.error('成交均价走势计算失败:', e)
    return { mode: 'none' }
  }
})

/** 同板块真实成交（最近30条，不含本楼盘） */
async function fetchPlateDeals() {
  plateLoaded.value = false
  try {
    const res = await request.get(`/public/loupans/${id.value}/real-deals/plate`) || {}
    plateName.value = res?.plate || ''
    plateDeals.value = res?.records || []
  } catch {
    plateName.value = ''
    plateDeals.value = []
  } finally { plateLoaded.value = true }
}

function fmt(t) { if (!t) return ''; const d = new Date(t); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }

// ===== 户型页：标准户型图 + 关联的视频素材 =====
/** 汇总单个户型的补充素材：标准户型图 + 素材库中 huxingId 匹配的短视频 */
function hxExtras(hx) {
  const list = []
  if (hx.standardHuxingImage) list.push({ kind: 'image', label: '标准户型图', url: hx.standardHuxingImage })
  medias.value
    .filter(m => m.huxingId === hx.id && m.mediaType === 5)
    .forEach(m => list.push({ kind: 'video', label: '户型视频', url: m.mediaUrl }))
  return list
}
const huxingVideoVisible = ref(false)
const huxingVideoUrl = ref('')
/** 点击补充素材：图片走全屏预览，视频走弹窗播放 */
function previewHuxingExtra(hx, ex) {
  if (ex.kind === 'video') {
    huxingVideoUrl.value = ex.url
    huxingVideoVisible.value = true
  } else {
    viewerImages.value = [ex.url]
    viewerIndex.value = 0
    viewerVisible.value = true
  }
}

// ===== 图片全屏预览 =====
const viewerVisible = ref(false)
const viewerImages = ref([])
const viewerIndex = ref(0)
function previewImages(images, index) {
  viewerImages.value = (images || '').split(',').filter(Boolean)
  viewerIndex.value = index || 0
  viewerVisible.value = true
}

// 图库图片预览：收集分组内可预览的图片，定位到点击的那张
function previewMedia(items, current) {
  if (current.mediaType === 5 || current.mediaType === 6) return
  const urls = items.filter(m => m.mediaType !== 5 && m.mediaType !== 6).map(m => m.mediaUrl)
  viewerImages.value = urls
  viewerIndex.value = urls.indexOf(current.mediaUrl)
  viewerVisible.value = true
}

/** 按当前子页面类型加载数据（先清空旧数据避免残留） */
function loadCurrent() {
  loupan.value = null
  medias.value = []
  huxings.value = []
  yfyjList.value = []
  dynamics.value = []
  realDeals.value = []
  plateDeals.value = []
  plateName.value = ''
  yfyjBuilding.value = ''
  if (subType.value === 'media' || subType.value === 'huxing') fetchMedias()
  if (subType.value === 'huxing') fetchHuxings()
  else if (subType.value === 'yfyj') fetchYfyj()
  else if (subType.value === 'dynamic') fetchDynamics()
  else if (subType.value === 'real-deal') { fetchRealDeals(); fetchPlateDeals() }
}

// 监听路由变化（首次进入 immediate + 子页面/楼盘切换时重新加载），避免 SPA 内切换组件复用导致空白
watch([id, subType], () => {
  fetchDetail()
  loadCurrent()
}, { immediate: true })
</script>
