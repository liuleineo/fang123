<template>
  <div v-if="loupan" class="space-y-6">
    <!-- 楼盘信息 -->
    <div v-if="showInfo">
      <h2 v-if="showMainTitle" class="text-lg font-bold text-[var(--color-text-primary)] mb-5 flex items-center gap-2">
        <Info class="w-5 h-5 text-[var(--color-primary)]" />楼盘信息
      </h2>

      <!-- 基本信息 -->
      <div class="mb-8">
        <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
          <Info class="w-4 h-4 text-[var(--color-primary)]" />基本信息
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          <div v-if="['','精装','毛坯','简装'][loupan.decorateType]" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">装修情况</span><span class="text-[var(--color-text-primary)] font-medium">{{ ['','精装','毛坯','简装'][loupan.decorateType] }}</span></div>
          <div v-if="loupan.openingDate" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">开盘时间</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.openingDate }}</span></div>
          <div v-if="loupan.deliveryDate" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">交房时间</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.deliveryDate }}</span></div>
          <div v-if="loupan.areaMin||loupan.areaMax" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">户型面积</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.areaMin }}-{{ loupan.areaMax }}㎡</span></div>
          <div v-if="['','住宅','公寓','商铺','别墅'][loupan.houseType]" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">楼盘类型</span><span class="text-[var(--color-text-primary)] font-medium">{{ ['','住宅','公寓','商铺','别墅'][loupan.houseType] }}</span></div>
          <div v-if="loupan.propertyRightYear" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">产权年限</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.propertyRightYear }}年</span></div>
          <div v-if="loupan.floorHeightMin||loupan.floorHeightMax" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">层高</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.floorHeightMin }}-{{ loupan.floorHeightMax }}m</span></div>
        </div>
      </div>

      <!-- 建筑指标 -->
      <div class="mb-8">
        <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
          <Building2 class="w-4 h-4 text-[var(--color-primary)]" />建筑指标
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          <div v-if="loupan.buildArea" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">总建面积</span><span class="text-[var(--color-text-primary)] font-medium">{{ fmtNum(loupan.buildArea) }}㎡</span></div>
          <div v-if="loupan.landArea" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">占地面积</span><span class="text-[var(--color-text-primary)] font-medium">{{ fmtNum(loupan.landArea) }}㎡</span></div>
          <div v-if="loupan.plotRatio" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">容积率</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.plotRatio }}</span></div>
          <div v-if="loupan.greenRate" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">绿地率</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.greenRate }}%</span></div>
          <div v-if="loupan.houseTotal" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">总户数</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.houseTotal }}户</span></div>
          <div v-if="loupan.buildingTotal" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">楼栋总数</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.buildingTotal }}栋</span></div>
          <div v-if="loupan.floorMin||loupan.floorMax" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">楼层范围</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.floorMin }}-{{ loupan.floorMax }}层</span></div>
          <div v-if="loupan.selfHoldRate" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">自持率</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.selfHoldRate }}%</span></div>
        </div>
      </div>

      <!-- 开发信息 -->
      <div class="mb-8">
        <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
          <Shield class="w-4 h-4 text-[var(--color-primary)]" />开发信息
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          <div v-if="loupan.projectCompany" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">开发公司</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.projectCompany }}</span></div>
          <div v-if="loupan.brandList" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">开发品牌</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.brandList }}</span></div>
          <div v-if="loupan.landPrice" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">拿地总价</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.landPrice }}万</span></div>
          <div v-if="loupan.landUnitPrice" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">楼面单价</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.landUnitPrice }}元/㎡</span></div>
          <div v-if="loupan.landBuyDate" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">拿地日期</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.landBuyDate }}</span></div>
          <div v-if="loupan.propertyCompany" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">物业公司</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.propertyCompany }}</span></div>
          <div v-if="loupan.propertyFeeHigh" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">小高/洋房物业费</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.propertyFeeHigh }}元/㎡/月</span></div>
          <div v-if="loupan.propertyFeeVilla" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">排屋别墅物业费</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.propertyFeeVilla }}元/㎡/月</span></div>
        </div>
      </div>

      <!-- 车位信息 -->
      <div class="mb-8">
        <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
          <Car class="w-4 h-4 text-[var(--color-primary)]" />车位信息
        </h3>
        <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div v-if="loupan.parkTotal" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">总车位</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.parkTotal }}个</span></div>
          <div v-if="loupan.parkSellNum" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">可售车位</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.parkSellNum }}个</span></div>
          <div v-if="loupan.parkRatio" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">车位配比</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.parkRatio }}</span></div>
          <div class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">人车分流</span><span class="text-[var(--color-text-primary)] font-medium">{{ loupan.peopleCarSeparate===1?'是':'否' }}</span></div>
        </div>
      </div>

      <!-- 外立面 -->
      <div v-if="loupan.facadeMaterial" class="mb-8">
        <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
          <Paintbrush class="w-4 h-4 text-[var(--color-primary)]" />外立面材料
        </h3>
        <p class="text-sm text-[var(--color-text-secondary)] leading-relaxed">{{ loupan.facadeMaterial }}</p>
      </div>

      <!-- 样板房说明 -->
      <div v-if="loupan.showHouseDesc" class="mb-8">
        <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
          <Eye class="w-4 h-4 text-[var(--color-primary)]" />样板房说明
        </h3>
        <p class="text-sm text-[var(--color-text-secondary)] leading-relaxed">{{ loupan.showHouseDesc }}</p>
      </div>

      <!-- 小区配套 -->
      <div v-if="splitItems(loupan.communityFacility).length">
        <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
          <Sparkles class="w-4 h-4 text-[var(--color-primary)]" />小区配套
        </h3>
        <ul class="grid grid-cols-2 gap-x-4 gap-y-1 list-none"><li v-for="(it,i) in splitItems(loupan.communityFacility)" :key="i" class="text-sm text-[var(--color-text-secondary)] before:content-['•'] before:mr-1.5 before:text-[var(--color-primary)]">{{ it.trim() }}</li></ul>
      </div>
    </div>

    <!-- 周边配套 -->
    <div v-if="showSupport">
      <h2 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
        <Sparkles class="w-5 h-5 text-[var(--color-primary)]" />周边配套
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div v-if="splitItems(loupan.eduSupport).length" class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-medium text-[var(--color-text-primary)] mb-2">教育</h4>
          <ul class="space-y-1 list-none"><li v-for="(it,i) in splitItems(loupan.eduSupport)" :key="i" class="text-xs text-[var(--color-text-secondary)] before:content-['•'] before:mr-1.5 before:text-[var(--color-primary)]">{{ it.trim() }}</li></ul>
        </div>
        <div v-if="splitItems(loupan.trafficSupport).length" class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-medium text-[var(--color-text-primary)] mb-2">交通</h4>
          <ul class="space-y-1 list-none"><li v-for="(it,i) in splitItems(loupan.trafficSupport)" :key="i" class="text-xs text-[var(--color-text-secondary)] before:content-['•'] before:mr-1.5 before:text-[var(--color-primary)]">{{ it.trim() }}</li></ul>
        </div>
        <div v-if="splitItems(loupan.medicalSupport).length" class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-medium text-[var(--color-text-primary)] mb-2">医疗</h4>
          <ul class="space-y-1 list-none"><li v-for="(it,i) in splitItems(loupan.medicalSupport)" :key="i" class="text-xs text-[var(--color-text-secondary)] before:content-['•'] before:mr-1.5 before:text-[var(--color-primary)]">{{ it.trim() }}</li></ul>
        </div>
        <div v-if="splitItems(loupan.businessSupport).length" class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-medium text-[var(--color-text-primary)] mb-2">商业</h4>
          <ul class="space-y-1 list-none"><li v-for="(it,i) in splitItems(loupan.businessSupport)" :key="i" class="text-xs text-[var(--color-text-secondary)] before:content-['•'] before:mr-1.5 before:text-[var(--color-primary)]">{{ it.trim() }}</li></ul>
        </div>
        <div v-if="splitItems(loupan.viewSupport).length" class="bg-gray-50 rounded-xl p-4">
          <h4 class="text-sm font-medium text-[var(--color-text-primary)] mb-2">景观</h4>
          <ul class="space-y-1 list-none"><li v-for="(it,i) in splitItems(loupan.viewSupport)" :key="i" class="text-xs text-[var(--color-text-secondary)] before:content-['•'] before:mr-1.5 before:text-[var(--color-primary)]">{{ it.trim() }}</li></ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Info, Building2, Shield, Car, Paintbrush, Eye, Sparkles } from 'lucide-vue-next'

defineProps({
  /** 楼盘详情对象（后端 /public/loupans/{id} 返回） */
  loupan: { type: Object, default: null },
  /** 是否渲染「楼盘信息」整块（详情页改为楼盘动态后传 false） */
  showInfo: { type: Boolean, default: true },
  /** 是否渲染「楼盘信息」大标题（子页面顶部已有标题时传 false） */
  showMainTitle: { type: Boolean, default: true },
  /** 是否渲染「周边配套」 */
  showSupport: { type: Boolean, default: true }
})

function fmtNum(n) {
  if (!n) return '0'
  return Number(n).toLocaleString()
}

/** 拆分配套设施文本 */
function splitItems(str) {
  return (str || '').split(/[,，、]/).filter(Boolean)
}
</script>
