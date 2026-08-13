<template>
  <div class="loupan-sub-page">
    <!-- 面包屑 -->
    <div class="section-container py-4">
      <t-breadcrumb>
        <t-breadcrumb-item :to="`/loupan/${route.params.id}`">楼盘详情</t-breadcrumb-item>
        <t-breadcrumb-item>{{ title }}</t-breadcrumb-item>
      </t-breadcrumb>
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
                <div v-for="m in group.items" :key="m.id" class="aspect-[4/3] rounded-xl bg-gray-100 overflow-hidden">
                  <t-image v-if="m.mediaType!==5&&m.mediaType!==6" :src="m.mediaUrl" fit="cover" class="w-full h-full" />
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
              <div v-if="hx.huxingImage" class="aspect-[4/3] bg-gray-50">
                <t-image :src="hx.huxingImage" fit="cover" class="w-full h-full" />
              </div>
              <div class="p-5 pt-0">
                <p class="text-2xl font-bold text-[var(--color-primary)] mb-1">{{ hx.area }}<span class="text-sm font-normal text-[var(--color-text-tertiary)]">㎡</span></p>
                <p class="text-sm text-[var(--color-text-secondary)] mb-3">{{ hx.roomNum }}室{{ hx.hallNum }}厅{{ hx.toiletNum }}卫
                  <span v-if="hx.balconyNum">· {{ hx.balconyNum }}阳台</span>
                </p>
                <div class="flex flex-wrap gap-1.5 text-xs">
                  <span class="px-2 py-0.5 rounded bg-blue-50 text-[var(--color-primary)]">{{ ['','小高层','洋房','叠墅','排屋'][hx.floorType] }}</span>
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
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Images, LayoutGrid, BadgeCent } from 'lucide-vue-next'
import request from '@/utils/request'

const route = useRoute()
const loupan = ref(null)
const huxings = ref([])
const medias = ref([])
const yfyjList = ref([])
const huxingLoading = ref(false)
const mediaLoading = ref(false)
const yfyjLoading = ref(false)
const yfyjBuilding = ref('')

// 根据路由 name 判断子页面类型
const subType = computed(() => {
  if (route.name === 'LoupanMedia') return 'media'
  if (route.name === 'LoupanHuxing') return 'huxing'
  if (route.name === 'LoupanYfyj') return 'yfyj'
  return ''
})

const titleMap = {
  media: '楼盘图库',
  huxing: '楼盘户型',
  yfyj: '一房一价'
}
const title = computed(() => titleMap[subType.value] || '')

const iconMap = {
  media: Images,
  huxing: LayoutGrid,
  yfyj: BadgeCent
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

onMounted(async () => {
  fetchDetail()
  if (subType.value === 'media') fetchMedias()
  else if (subType.value === 'huxing') fetchHuxings()
  else if (subType.value === 'yfyj') fetchYfyj()
})
</script>
