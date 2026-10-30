<template>
  <div class="loupan-detail-page" v-if="loupan">
    <!-- 面包屑 -->
    <div class="section-container py-4">
      <t-breadcrumb>
        <t-breadcrumb-item to="/">首页</t-breadcrumb-item>
        <t-breadcrumb-item>{{ loupan.district }}</t-breadcrumb-item>
        <t-breadcrumb-item>{{ loupan.projectName }}</t-breadcrumb-item>
      </t-breadcrumb>
    </div>

    <!-- 封面大图 -->
    <div v-if="loupan.coverImage" class="section-container mb-6">
      <img :src="loupan.coverImage" class="w-full block rounded-xl" style="display:block" />
    </div>

    <!-- 头部信息 -->
    <section class="bg-white border-b border-gray-50">
      <div class="section-container py-8">
        <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-3">
              <h1 class="text-2xl sm:text-3xl font-bold text-[var(--color-text-primary)]">{{ loupan.projectName }}</h1>
              <span v-if="loupan.brandList" class="px-2.5 py-1 rounded-full text-xs font-medium whitespace-nowrap bg-orange-50 text-orange-600 border border-orange-100">{{ loupan.brandList }}</span>
              <span :class="['px-2.5 py-1 rounded-full text-xs font-medium whitespace-nowrap',
                loupan.salesStatus===1?'bg-green-100 text-green-700':
                loupan.salesStatus===2?'bg-gray-100 text-gray-500':
                loupan.salesStatus===3?'bg-blue-100 text-blue-700':
                'bg-gray-100 text-gray-600']">
                {{ ['待售','在售','售罄','已交付'][loupan.salesStatus] || '未知' }}
              </span>
            </div>

            <div class="flex flex-wrap items-center gap-2 mb-4 text-sm text-[var(--color-text-tertiary)]">
              <span class="flex items-center gap-1"><MapPin class="w-4 h-4" />{{ loupan.district }}{{ loupan.plate ? '·'+loupan.plate : '' }}</span>
              <span class="text-gray-200">|</span>
              <span>{{ ['','住宅','公寓','商铺','别墅'][loupan.houseType] || '' }}</span>
              <span class="text-gray-200">|</span>
              <span>{{ ['','精装','毛坯','简装'][loupan.decorateType] || '' }} · {{ loupan.propertyRightYear }}年产权</span>
            </div>

            <!-- 价格信息 -->
            <div class="flex items-baseline gap-6 mb-2">
              <div v-if="loupan.avgUnitPrice">
                <span class="text-3xl font-bold text-[var(--color-danger)]">{{ loupan.avgUnitPrice }}</span>
                <span class="text-sm text-[var(--color-text-tertiary)] ml-1">元/㎡（高层均价）</span>
              </div>
              <div v-if="loupan.minTotalPrice || loupan.maxTotalPrice">
                <span class="text-lg font-bold text-[var(--color-text-primary)]">
                  {{ loupan.minTotalPrice }}-{{ loupan.maxTotalPrice }}万
                </span>
                <span class="text-sm text-[var(--color-text-tertiary)] ml-1">（总价）</span>
              </div>
              <div v-if="!loupan.avgUnitPrice && !loupan.minTotalPrice" class="text-sm text-[var(--color-text-tertiary)]">
                价格待定
              </div>
            </div>

            <!-- 洋房/叠墅/排屋均价 -->
            <div v-if="loupan.avgUnitPriceYangfang || loupan.avgUnitPriceDieshu || loupan.avgUnitPricePaiwu" class="flex flex-wrap gap-x-5 gap-y-1 mb-2 text-sm">
              <span v-if="loupan.avgUnitPriceYangfang" class="text-[var(--color-text-secondary)]">
                洋房均价：<span class="text-[var(--color-danger)] font-bold">{{ loupan.avgUnitPriceYangfang }}</span>元/㎡
              </span>
              <span v-if="loupan.avgUnitPriceDieshu" class="text-[var(--color-text-secondary)]">
                叠墅均价：<span class="text-[var(--color-danger)] font-bold">{{ loupan.avgUnitPriceDieshu }}</span>元/㎡
              </span>
              <span v-if="loupan.avgUnitPricePaiwu" class="text-[var(--color-text-secondary)]">
                排屋均价：<span class="text-[var(--color-danger)] font-bold">{{ loupan.avgUnitPricePaiwu }}</span>元/㎡
              </span>
            </div>

            <!-- 标签 -->
            <div v-if="loupan.priceTag" class="flex flex-wrap gap-1.5 mb-4">
              <span v-for="tag in loupan.priceTag.split(',')" :key="tag" class="px-2.5 py-1 rounded text-xs bg-orange-50 text-orange-600 border border-orange-100">{{ tag }}</span>
            </div>

            <!-- 快捷信息 -->
            <div class="flex items-center gap-4 text-sm">
              <span v-if="loupan.deliveryDate" class="text-[var(--color-text-tertiary)]">
                交房时间：{{ loupan.deliveryDate }}
              </span>
            </div>
          </div>

        </div>

        <!-- 快捷入口按钮：楼盘信息 / 图库 / 户型 / 一房一价（移动端图标文字上下居中，PC 横向） -->
        <div class="grid grid-cols-3 gap-2 pt-5 pb-5 border-t border-gray-50 sm:flex sm:flex-wrap">
          <t-button theme="primary" variant="outline" class="!rounded-xl !h-auto !py-2.5" @click="$router.push(`/loupan/${route.params.id}/info`)">
            <span class="w-full flex flex-col items-center justify-center gap-0.5 sm:flex-row sm:gap-1.5">
              <Info class="w-5 h-5 sm:w-4 sm:h-4" />
              <span class="text-xs leading-none sm:text-sm">楼盘信息</span>
            </span>
          </t-button>
          <t-button theme="primary" variant="outline" class="!rounded-xl !h-auto !py-2.5" @click="$router.push(`/loupan/${route.params.id}/media`)">
            <span class="w-full flex flex-col items-center justify-center gap-0.5 sm:flex-row sm:gap-1.5">
              <Images class="w-5 h-5 sm:w-4 sm:h-4" />
              <span class="text-xs leading-none sm:text-sm">图库视频</span>
            </span>
          </t-button>
          <t-button theme="primary" variant="outline" class="!rounded-xl !h-auto !py-2.5" @click="$router.push(`/loupan/${route.params.id}/huxing`)">
            <span class="w-full flex flex-col items-center justify-center gap-0.5 sm:flex-row sm:gap-1.5">
              <LayoutGrid class="w-5 h-5 sm:w-4 sm:h-4" />
              <span class="text-xs leading-none sm:text-sm">户型</span>
            </span>
          </t-button>
          <t-button theme="primary" variant="outline" class="!rounded-xl !h-auto !py-2.5" @click="$router.push(`/loupan/${route.params.id}/yfyj`)">
            <span class="w-full flex flex-col items-center justify-center gap-0.5 sm:flex-row sm:gap-1.5">
              <BadgeCent class="w-5 h-5 sm:w-4 sm:h-4" />
              <span class="text-xs leading-none sm:text-sm">一房一价</span>
            </span>
          </t-button>
          <t-button theme="primary" variant="outline" class="!rounded-xl !h-auto !py-2.5" @click="$router.push(`/loupan/${route.params.id}/dynamic`)">
            <span class="w-full flex flex-col items-center justify-center gap-0.5 sm:flex-row sm:gap-1.5">
              <Newspaper class="w-5 h-5 sm:w-4 sm:h-4" />
              <span class="text-xs leading-none sm:text-sm">楼盘动态</span>
            </span>
          </t-button>
          <t-button theme="primary" variant="outline" class="!rounded-xl !h-auto !py-2.5" @click="$router.push(`/loupan/${route.params.id}/real-deal`)">
            <span class="w-full flex flex-col items-center justify-center gap-0.5 sm:flex-row sm:gap-1.5">
              <HandCoins class="w-5 h-5 sm:w-4 sm:h-4" />
              <span class="text-xs leading-none sm:text-sm">二手真实成交</span>
            </span>
          </t-button>
        </div>
      </div>
    </section>

    <!-- 内容区：从上到下 相册 / 位置 / 户型 / 开盘信息 / 楼盘信息 / 周边配套 -->
    <section class="py-6 bg-[#FFF] min-h-[60vh]">
      <div class="section-container space-y-6">

        <!-- 0. 楼盘相册（最多6张，点击更多进入图库） -->
        <div v-if="albumPhotos.length" class="">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-bold text-[var(--color-text-primary)] flex items-center gap-2">
              <Images class="w-5 h-5 text-[var(--color-primary)]" />楼盘相册
            </h2>
            <button @click="$router.push(`/loupan/${route.params.id}/media`)" class="text-sm text-[var(--color-primary)] flex items-center gap-0.5 hover:opacity-80 transition-opacity">
              更多<ChevronRight class="w-4 h-4" />
            </button>
          </div>
          <div class="grid grid-cols-3 sm:grid-cols-6 gap-2">
            <img v-for="(u,i) in albumPhotos" :key="i" :src="u" loading="lazy"
                 class="w-full aspect-square object-cover rounded-lg cursor-pointer hover:opacity-90 transition-opacity"
                 @click="previewAlbum(i)" />
          </div>
        </div>

        <!-- 1. 楼盘位置 -->
        <div class="">
          <h2 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
            <MapPin class="w-5 h-5 text-[var(--color-primary)]" />楼盘位置
          </h2>
          <div v-if="loupan.longitude && loupan.latitude" id="amap-container" class="w-full h-64 sm:h-80 rounded-xl bg-gray-100"></div>
          <div v-else class="text-center py-10 text-[var(--color-text-tertiary)]">暂无位置信息</div>
        </div>

        <!-- 2. 户型信息（横滑展示前几个，完整见子页面） -->
        <div class="">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-bold text-[var(--color-text-primary)] flex items-center gap-2">
              <Building2 class="w-5 h-5 text-[var(--color-primary)]" />户型信息
            </h2>
            <t-link theme="primary" @click="$router.push(`/loupan/${route.params.id}/huxing`)">查看全部 ›</t-link>
          </div>
          <div v-if="huxingLoading" class="text-center py-10"><t-loading /></div>
          <div v-else-if="!huxings.length" class="text-center py-10 text-[var(--color-text-tertiary)]">暂无户型信息</div>
          <div v-else class="flex gap-3 overflow-x-auto pb-2">
            <div v-for="hx in huxingCards" :key="hx.id" class="flex-shrink-0 w-44 border border-gray-100 rounded-lg overflow-hidden">
              <div class="aspect-[4/3] bg-gray-50 relative group" :class="{ 'cursor-zoom-in': hx.huxingImage }"
                   @click="previewHuxingImage(hx)">
                <t-image v-if="hx.huxingImage" :src="hx.huxingImage" fit="contain" class="w-full h-full" />
                <span v-else class="absolute inset-0 flex items-center justify-center text-gray-300 text-xs">暂无图</span>
                <!-- 悬浮放大提示：不影响点击 -->
                <span v-if="hx.huxingImage"
                      class="pointer-events-none absolute right-1.5 bottom-1.5 px-1.5 py-0.5 rounded bg-black/45 text-[11px] leading-none text-white opacity-0 group-hover:opacity-100 transition-opacity">
                  点击放大
                </span>
              </div>
              <div class="p-2.5">
                <p class="text-sm font-bold text-[var(--color-text-primary)] truncate">{{ hx.huxingName }}</p>
                <p class="text-xs text-[var(--color-text-secondary)]">{{ hx.area }}㎡ {{ hx.roomNum }}室{{ hx.hallNum }}厅</p>
                <div v-if="hx.unitPrice" class="text-xs text-[var(--color-primary)] font-medium">{{ hx.unitPrice }}元/㎡</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 3. 开盘信息 -->
        <div class="">
          <h2 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
            <FileText class="w-5 h-5 text-[var(--color-primary)]" />开盘信息
          </h2>
          <div v-if="presaleLoading" class="text-center py-10"><t-loading /></div>
          <div v-else-if="!presaleList.length" class="text-center py-10 text-[var(--color-text-tertiary)]">暂无开盘信息</div>
          <div v-else class="overflow-x-auto">
            <table class="w-full text-sm border-collapse">
              <thead>
                <tr class="bg-gray-50 text-left text-[var(--color-text-secondary)]">
                  <th class="p-3 font-medium">预售证编号</th>
                  <th class="p-3 font-medium">坐落位置</th>
                  <th class="p-3 font-medium">公示日期</th>
                  <th class="p-3 font-medium">核发日期</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in presaleList" :key="item.id" class="border-t border-gray-100 hover:bg-gray-50">
                  <td class="p-3 font-medium text-[var(--color-primary)]">{{ item.permitNoStr||item.permitNo }}</td>
                  <td class="p-3 text-[var(--color-text-tertiary)] text-xs max-w-[160px] truncate">{{ item.location||'-' }}</td>
                  <td class="p-3 text-[var(--color-text-secondary)]">{{ item.publicityDate||'-' }}</td>
                  <td class="p-3 text-[var(--color-text-secondary)]">{{ item.issueDate||'-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 4. 最近 5 条楼盘动态（完整列表见 /loupan/:id/dynamic） -->
        <div class="">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-bold text-[var(--color-text-primary)] flex items-center gap-2">
              <Newspaper class="w-5 h-5 text-[var(--color-primary)]" />楼盘动态
            </h2>
            <span class="text-xs text-[var(--color-text-tertiary)]">最近 5 条</span>
          </div>

          <div v-if="dynamicLoading" class="text-center py-10"><t-loading /></div>
          <div v-else-if="!dynamics.length" class="text-center py-10 text-[var(--color-text-tertiary)]">暂无楼盘动态</div>
          <div v-else class="space-y-3">
            <div v-for="d in dynamics.slice(0, 5)" :key="d.id" class="border border-gray-100 rounded-xl p-4">
              <div class="flex items-center gap-2 mb-2">
                <t-tag size="small" :theme="typeTheme[d.type] || 'default'">{{ typeMap[d.type] || '动态' }}</t-tag>
                <span class="flex-1 min-w-0 text-sm font-bold text-[var(--color-text-primary)] truncate">{{ d.title }}</span>
                <span class="shrink-0 text-xs text-[var(--color-text-tertiary)]">{{ fmtDate(d.createTime) }}</span>
              </div>
              <div v-if="photosOf(d).length" class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 gap-2 mb-2">
                <img v-for="(url, i) in photosOf(d).slice(0, 6)" :key="i" :src="url" loading="lazy"
                     class="w-full aspect-[4/3] object-cover rounded-lg border border-gray-100 cursor-pointer hover:opacity-90 transition-opacity"
                     @error="e => (e.target.style.display = 'none')"
                     @click="previewDynamic(d, i)" />
              </div>
              <p v-if="d.content" class="text-sm leading-6 text-[var(--color-text-secondary)] whitespace-pre-wrap line-clamp-3">{{ d.content }}</p>
            </div>
          </div>

          <t-button v-if="dynamics.length" block variant="outline" class="!mt-4"
                    @click="$router.push(`/loupan/${route.params.id}/dynamic`)">
            查看更多动态
            <ChevronRight class="inline-block w-4 h-4 ml-1 -mt-0.5" />
          </t-button>
        </div>

        <!-- 5. 周边配套（与 /loupan/:id/info 子页面共用同一组件，这里只渲染配套） -->
        <LoupanInfoBlocks :loupan="loupan" :show-info="false" />

      </div>
    </section>
  </div>

  <!-- 加载中 -->
  <div v-else class="flex justify-center py-40">
    <t-loading size="large" text="加载中..." />
  </div>

  <!-- 图片全屏预览 -->
  <t-image-viewer v-model:visible="viewerVisible" :images="viewerImages" v-model:index="viewerIndex" />
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Info, Building2, MapPin, Images, LayoutGrid, BadgeCent, FileText, Newspaper, HandCoins, ChevronRight } from 'lucide-vue-next'
import request from '@/utils/request'
import LoupanInfoBlocks from '@/components/LoupanInfoBlocks.vue'

const route = useRoute()
const loupan = ref(null)
const huxings = ref([])
const presaleList = ref([])
const dynamics = ref([])
const huxingLoading = ref(false)
const presaleLoading = ref(false)
const dynamicLoading = ref(false)
const albumPhotos = ref([])
const viewerVisible = ref(false)
const viewerImages = ref([])
const viewerIndex = ref(0)
let amapInstance = null

async function fetchDetail() {
  try {
    loupan.value = await request.get(`/public/loupans/${route.params.id}`)
  } catch {}
}

async function fetchHuxings() {
  if (huxings.value.length) return
  huxingLoading.value = true
  try {
    huxings.value = await request.get(`/public/loupans/${route.params.id}/huxings`) || []
  } catch {} finally { huxingLoading.value = false }
}

async function fetchPresale() {
  if (presaleList.value.length) return
  presaleLoading.value = true
  try {
    presaleList.value = await request.get(`/public/loupans/${route.params.id}/presale-permits`) || []
  } catch {} finally { presaleLoading.value = false }
}

/** 楼盘相册：取图片类素材前 6 张（过滤短视频/VR） */
async function fetchAlbums() {
  try {
    const list = await request.get(`/public/loupans/${route.params.id}/medias`) || []
    const pics = list
      .filter(m => m.mediaUrl && m.mediaType !== 5 && m.mediaType !== 6)
      .map(m => m.mediaUrl)
    albumPhotos.value = pics.slice(0, 6)
  } catch {}
}

function previewAlbum(i) {
  viewerImages.value = albumPhotos.value
  viewerIndex.value = i
  viewerVisible.value = true
}

// ===== 户型信息：横滑展示前 6 个，图片点击全屏预览 =====
/** 取前 6 个户型并按面积倒序（保持原展示顺序） */
const huxingCards = computed(() => huxings.value.slice(0, 6).sort((a, b) => (b.area || 0) - (a.area || 0)))

/** 点击户型图：在本次展示的户型图之间左右切换（按 id 定位，避免多户型共用同一张图时索引错位） */
function previewHuxingImage(hx) {
  if (!hx?.huxingImage) return
  const items = huxingCards.value.filter(h => h.huxingImage).map(h => ({ id: h.id, url: h.huxingImage }))
  const idx = items.findIndex(it => it.id === hx.id)
  if (idx < 0) return
  viewerImages.value = items.map(it => it.url)
  viewerIndex.value = idx
  viewerVisible.value = true
}

// ===== 楼盘动态（详情页展示最近 5 条）=====
const typeMap = { 1: '建设动态', 2: '销售动态', 3: '优惠动态' }
const typeTheme = { 1: 'primary', 2: 'success', 3: 'warning' }

async function fetchDynamics() {
  dynamicLoading.value = true
  try {
    dynamics.value = await request.get(`/public/loupans/${route.params.id}/dynamics`) || []
  } catch {
    dynamics.value = []
  } finally {
    dynamicLoading.value = false
  }
}

/** 动态图片字段是逗号分隔的字符串 */
function photosOf(d) {
  return String(d?.images || '').split(',').map(s => s.trim()).filter(Boolean)
}

/** 点击动态图片：在当前动态的图片组内全屏预览 */
function previewDynamic(d, index) {
  const list = photosOf(d)
  if (!list.length) return
  viewerImages.value = list
  viewerIndex.value = index
  viewerVisible.value = true
}

function fmtDate(t) {
  if (!t) return ''
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t).slice(0, 10)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

// 高德地图初始化
async function initMap() {
  if (amapInstance || !loupan.value?.longitude || !loupan.value?.latitude) return
  await nextTick()
  const el = document.getElementById('amap-container')
  if (!el || typeof AMap === 'undefined') {
    setTimeout(initMap, 500)
    return
  }
  amapInstance = new AMap.Map('amap-container', {
    zoom: 12,
    center: [loupan.value.longitude, loupan.value.latitude],
    viewMode: '2D',
    resizeEnable: true
  })
  const marker = new AMap.Marker({
    position: [loupan.value.longitude, loupan.value.latitude],
    title: loupan.value.projectName
  })
  amapInstance.add(marker)
}

onMounted(async () => {
  await fetchDetail()
  fetchHuxings()
  fetchPresale()
  fetchAlbums()
  fetchDynamics()
  initMap()
})
</script>
