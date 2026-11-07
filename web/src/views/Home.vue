<template>
  <div class="home-page">
    <!-- Hero 搜索区 -->
    <section class="relative overflow-hidden bg-gradient-to-br from-[#F0F5FF] via-white to-[#E8F2FF] py-10 lg:py-14">
      <div class="absolute inset-0 overflow-hidden pointer-events-none">
        <div class="absolute -top-40 -right-40 w-96 h-96 bg-[#0052D9]/5 rounded-full blur-3xl" />
        <div class="absolute -bottom-40 -left-40 w-80 h-80 bg-[#618DFF]/5 rounded-full blur-3xl" />
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gradient-to-br from-blue-50/50 to-transparent rounded-full blur-3xl" />
      </div>

      <div class="section-container relative z-10 text-center">
        <h1 class="text-3xl sm:text-4xl lg:text-5xl font-bold text-[var(--color-text-primary)] leading-tight mb-4">
          杭房一二三，<br class="sm:hidden" />
          <span class="bg-gradient-to-r from-[#0052D9] to-[#618DFF] bg-clip-text text-transparent">让买房变简单。</span>
        </h1>
        <p class="text-sm sm:text-base text-[var(--color-text-tertiary)] max-w-xl mx-auto mb-6 leading-relaxed">
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

        <!-- 预售公示 / 最新开盘 -->
        <div class="max-w-md mx-auto mt-4 flex flex-wrap items-center justify-center gap-3">
          <button
            v-for="item in spotlightButtons"
            :key="item.type"
            class="relative inline-flex items-center gap-2 bg-white rounded-full border px-5 py-2.5 shadow-sm transition-all hover:shadow-md hover:-translate-y-0.5"
            :class="spotlightType === item.type
              ? 'border-[var(--color-primary)] ring-2 ring-[#0052D9]/15'
              : 'border-gray-100'"
            @click="toggleSpotlight(item.type)"
          >
            <span class="w-6 h-6 rounded-md flex items-center justify-center" :class="item.iconCls">
              <component :is="item.icon" class="w-4 h-4" />
            </span>
            <span class="text-sm font-medium text-[var(--color-text-primary)]">{{ item.label }}</span>
            <!-- 右上角数量角标 -->
            <span
              class="absolute -top-1.5 -right-1.5 min-w-[18px] h-[18px] px-1 flex items-center justify-center rounded-full text-[11px] font-bold leading-none text-white shadow"
              :class="item.badgeCls"
            >{{ spotlightCounts[item.type] ?? 0 }}</span>
          </button>
        </div>

        <!-- 热门标签 -->
        <div class="flex flex-wrap items-center justify-center gap-2 mt-4">
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

    <!-- 楼盘筛选 + 列表：已抽成公共组件，与独立页 /xinpan 共用 -->
    <LoupanListSection ref="listRef" v-model:spotlight="spotlightType" :show-keyword="true" />

    <!-- CTA -->
    <section class="py-16 bg-gradient-to-r from-[#0052D9] to-[#366EF4]">
      <div class="section-container text-center">
        <h2 class="text-2xl sm:text-3xl font-bold text-white mb-4">没有合适的新楼盘？看看二手房</h2>
        
        <t-button theme="default" size="large" variant="outline" class="!border-white !text-black !rounded-xl hover:!bg-white hover:!text-[#0052D9]" @click="goErshoufang">
          浏览二手楼盘
        </t-button>
        
        <p class="text-white/80 mt-6 mb-8 text-sm max-w-md mx-auto">
          也欢迎直接咨询经纪人，为您推荐好房。
        </p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Megaphone, Rocket } from 'lucide-vue-next'
import request from '@/utils/request'
import LoupanListSection from '@/components/LoupanListSection.vue'

const router = useRouter()
/** 筛选 + 楼盘列表 + 分页都在公共组件里，首页只负责 Hero 区 */
const listRef = ref(null)

const searchKeyword = ref('')
const hotTags = ['钱江新城', '金沙湖', '绿城', '滨江']

/** 预售公示 / 最新开盘 板块（数据源：预售证信息表） */
const spotlightType = ref('')
const spotlightCounts = ref({ presale: 0, opening: 0 })
const spotlightButtons = [
  { type: 'presale', label: '预售公示', icon: Megaphone, iconCls: 'bg-amber-50 text-amber-600', badgeCls: 'bg-amber-500' },
  { type: 'opening', label: '最新开盘', icon: Rocket, iconCls: 'bg-blue-50 text-[var(--color-primary)]', badgeCls: 'bg-[#0052D9]' }
]

/** 两个板块的楼盘数量（按钮右上角角标） */
async function fetchSpotlightCounts() {
  try {
    const r = await request.get('/public/loupan-spotlight/counts')
    spotlightCounts.value = { presale: r?.presale || 0, opening: r?.opening || 0 }
  } catch {}
}

/** 首页搜索框 / 热门标签 → 交给列表面板搜索 */
function doSearch() {
  listRef.value?.search(searchKeyword.value)
}

/** 首页板块按钮 → 列表面板切换板块模式 */
function toggleSpotlight(type) {
  listRef.value?.toggleSpotlight(type)
}

/** 底部 CTA：首页没有合适的新楼盘时，引导去二手房页面 */
function goErshoufang() {
  router.push('/ershoufang')
}

onMounted(() => { fetchSpotlightCounts() })
</script>
