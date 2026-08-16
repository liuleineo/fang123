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

        <!-- 快捷入口按钮：图库 / 户型 / 一房一价 -->
        <div class="flex gap-2 flex-wrap mt-6 pt-5 border-t border-gray-50">
          <t-button theme="primary" variant="outline" class="!rounded-xl flex-1 sm:flex-none" @click="$router.push(`/loupan/${route.params.id}/media`)">
            <Images class="w-4 h-4 mr-1" />图库
          </t-button>
          <t-button theme="primary" variant="outline" class="!rounded-xl flex-1 sm:flex-none" @click="$router.push(`/loupan/${route.params.id}/huxing`)">
            <LayoutGrid class="w-4 h-4 mr-1" />户型
          </t-button>
          <t-button theme="primary" variant="outline" class="!rounded-xl flex-1 sm:flex-none" @click="$router.push(`/loupan/${route.params.id}/yfyj`)">
            <BadgeCent class="w-4 h-4 mr-1" />一房一价
          </t-button>
        </div>
      </div>
    </section>

    <!-- 内容区：从上到下 位置 / 户型 / 开盘信息 / 楼盘信息 / 周边配套 -->
    <section class="py-6 bg-[#F8FAFE] min-h-[60vh]">
      <div class="section-container space-y-6">

        <!-- 1. 楼盘位置 -->
        <div class="bg-white rounded-xl border border-gray-100 p-5 sm:p-6">
          <h2 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
            <MapPin class="w-5 h-5 text-[var(--color-primary)]" />楼盘位置
          </h2>
          <div v-if="loupan.longitude && loupan.latitude" id="amap-container" class="w-full h-64 sm:h-80 rounded-xl bg-gray-100"></div>
          <div v-else class="text-center py-10 text-[var(--color-text-tertiary)]">暂无位置信息</div>
        </div>

        <!-- 2. 户型信息（横滑展示前几个，完整见子页面） -->
        <div class="bg-white rounded-xl border border-gray-100 p-5 sm:p-6">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-lg font-bold text-[var(--color-text-primary)] flex items-center gap-2">
              <Building2 class="w-5 h-5 text-[var(--color-primary)]" />户型信息
            </h2>
            <t-link theme="primary" @click="$router.push(`/loupan/${route.params.id}/huxing`)">查看全部 ›</t-link>
          </div>
          <div v-if="huxingLoading" class="text-center py-10"><t-loading /></div>
          <div v-else-if="!huxings.length" class="text-center py-10 text-[var(--color-text-tertiary)]">暂无户型信息</div>
          <div v-else class="flex gap-3 overflow-x-auto pb-2">
            <div v-for="hx in huxings.slice(0, 6).sort((a,b)=>(b.area||0)-(a.area||0))" :key="hx.id" class="flex-shrink-0 w-44 border border-gray-100 rounded-lg overflow-hidden">
              <div class="aspect-[4/3] bg-gray-50 relative">
                <t-image v-if="hx.huxingImage" :src="hx.huxingImage" fit="contain" class="w-full h-full" />
                <span v-else class="absolute inset-0 flex items-center justify-center text-gray-300 text-xs">暂无图</span>
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
        <div class="bg-white rounded-xl border border-gray-100 p-5 sm:p-6">
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
                  <th class="p-3 font-medium">公示结束日期</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in presaleList" :key="item.id" class="border-t border-gray-100 hover:bg-gray-50">
                  <td class="p-3 font-medium text-[var(--color-primary)]">{{ item.permitNoStr||item.permitNo }}</td>
                  <td class="p-3 text-[var(--color-text-tertiary)] text-xs max-w-[160px] truncate">{{ item.location||'-' }}</td>
                  <td class="p-3 text-[var(--color-text-secondary)]">{{ item.publicityEndDate||'-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 4. 楼盘信息 -->
        <div class="bg-white rounded-xl border border-gray-100 p-5 sm:p-6">
          <h2 class="text-lg font-bold text-[var(--color-text-primary)] mb-5 flex items-center gap-2">
            <Info class="w-5 h-5 text-[var(--color-primary)]" />楼盘信息
          </h2>

          <!-- 基本信息 -->
          <div class="mb-8">
            <h3 class="text-base font-bold text-[var(--color-text-primary)] mb-3 flex items-center gap-2">
              <Info class="w-4 h-4 text-[var(--color-primary)]" />基本信息
            </h3>
            <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              <div v-if="['','精装','毛坯','简装'][loupan.decorateType]" class="text-sm"><span class="text-[var(--color-text-tertiary)] block mb-0.5">装修情况</span><span class="text-[var(--color-text-primary)] font-medium">{{ ['','精装','毛坯','简装'][loupan.decorateType] }}</span></div>
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

        <!-- 5. 周边配套 -->
        <div class="bg-white rounded-xl border border-gray-100 p-5 sm:p-6">
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
    </section>
  </div>

  <!-- 加载中 -->
  <div v-else class="flex justify-center py-40">
    <t-loading size="large" text="加载中..." />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Info, Building2, Shield, Car, Sparkles, MapPin, Paintbrush, Eye, Images, LayoutGrid, BadgeCent, FileText } from 'lucide-vue-next'
import request from '@/utils/request'

const route = useRoute()
const loupan = ref(null)
const huxings = ref([])
const presaleList = ref([])
const huxingLoading = ref(false)
const presaleLoading = ref(false)
let amapInstance = null

function fmtNum(n) {
  if (!n) return '0'
  return Number(n).toLocaleString()
}

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
    zoom: 15,
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
  initMap()
})

/** 拆分配套设施文本 */
function splitItems(str) { return (str || '').split(/[,，、]/).filter(Boolean) }
</script>
