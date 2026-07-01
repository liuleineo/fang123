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

    <!-- 头部信息 -->
    <section class="bg-white border-b border-gray-50">
      <!-- 封面大图 -->
      <div v-if="loupan.coverImage" class="w-full h-64 sm:h-80 lg:h-96 bg-gray-100 overflow-hidden">
        <t-image :src="loupan.coverImage" fit="cover" class="w-full h-full" />
      </div>
      <div class="section-container py-8">
        <div class="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-6">
          <div class="flex-1">
            <div class="flex items-center gap-3 mb-3">
              <h1 class="text-2xl sm:text-3xl font-bold text-[var(--color-text-primary)]">{{ loupan.projectName }}</h1>
              <span :class="['px-2.5 py-1 rounded-full text-xs font-medium whitespace-nowrap',
                loupan.salesStatus===1?'bg-green-100 text-green-700':
                loupan.salesStatus===2?'bg-gray-100 text-gray-500':
                loupan.salesStatus===3?'bg-orange-100 text-orange-700':
                loupan.salesStatus===4?'bg-blue-100 text-blue-700':
                'bg-gray-100 text-gray-600']">
                {{ ['待开盘','在售','售罄','停工','已交付'][loupan.salesStatus] || '未知' }}
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
            <div class="flex items-baseline gap-6 mb-4">
              <div v-if="loupan.avgUnitPrice">
                <span class="text-3xl font-bold text-[var(--color-danger)]">{{ loupan.avgUnitPrice }}</span>
                <span class="text-sm text-[var(--color-text-tertiary)] ml-1">元/㎡（均价）</span>
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

            <!-- 标签 -->
            <div v-if="loupan.priceTag" class="flex flex-wrap gap-1.5 mb-4">
              <span v-for="tag in loupan.priceTag.split(',')" :key="tag" class="px-2.5 py-1 rounded text-xs bg-orange-50 text-orange-600 border border-orange-100">{{ tag }}</span>
            </div>

            <!-- 快捷信息 -->
            <div class="flex items-center gap-4 text-sm">
              <span v-if="loupan.salesTel" class="text-[var(--color-text-secondary)]">
                <Phone class="w-4 h-4 inline -mt-0.5 mr-1 text-[var(--color-primary)]" />{{ loupan.salesTel }}
              </span>
              <span v-if="loupan.projectCompany" class="text-[var(--color-text-tertiary)]">
                开发商：{{ loupan.projectCompany }}
              </span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div v-if="loupan.salesTel" class="flex gap-3 flex-shrink-0">
            <t-button variant="outline" class="!rounded-xl" @click="copyTel">
              <Phone class="w-4 h-4 mr-1" />拨打电话
            </t-button>
            <t-tooltip content="功能开发中">
              <t-button variant="outline" class="!rounded-xl">
                <Heart class="w-4 h-4 mr-1" />收藏
              </t-button>
            </t-tooltip>
          </div>
        </div>
      </div>
    </section>

    <!-- Tab 内容区 -->
    <section class="py-8 bg-[#F8FAFE] min-h-[60vh]">
      <div class="section-container">
        <t-tabs v-model="activeTab" size="medium" class="bg-white rounded-xl border border-gray-100">
          <!-- 楼盘详情 -->
          <t-tab-panel value="info" label="楼盘详情">
            <div class="p-6">
              <!-- 基本信息 -->
              <div class="mb-8">
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Info class="w-5 h-5 text-[var(--color-primary)]" />基本信息
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
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Building2 class="w-5 h-5 text-[var(--color-primary)]" />建筑指标
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
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Shield class="w-5 h-5 text-[var(--color-primary)]" />开发信息
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
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Car class="w-5 h-5 text-[var(--color-primary)]" />车位信息
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
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Paintbrush class="w-5 h-5 text-[var(--color-primary)]" />外立面材料
                </h3>
                <p class="text-sm text-[var(--color-text-secondary)] leading-relaxed">{{ loupan.facadeMaterial }}</p>
              </div>

              <!-- 样板房说明 -->
              <div v-if="loupan.showHouseDesc" class="mb-8">
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Eye class="w-5 h-5 text-[var(--color-primary)]" />样板房说明
                </h3>
                <p class="text-sm text-[var(--color-text-secondary)] leading-relaxed">{{ loupan.showHouseDesc }}</p>
              </div>

              <!-- 小区配套 -->
              <div v-if="splitItems(loupan.communityFacility).length" class="mb-8">
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Sparkles class="w-5 h-5 text-[var(--color-primary)]" />小区配套
                </h3>
                <ul class="grid grid-cols-2 gap-x-4 gap-y-1 list-none"><li v-for="(it,i) in splitItems(loupan.communityFacility)" :key="i" class="text-sm text-[var(--color-text-secondary)] before:content-['•'] before:mr-1.5 before:text-[var(--color-primary)]">{{ it.trim() }}</li></ul>
              </div>

            </div>
          </t-tab-panel>

          <!-- 户型 -->
          <t-tab-panel value="huxing" label="户型信息">
            <div class="p-6">
              <div v-if="huxingLoading" class="text-center py-10"><t-loading /></div>
              <div v-else-if="!huxings.length" class="text-center py-10 text-[var(--color-text-tertiary)]">暂无户型信息</div>
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
                      <span v-if="hx.orientation">· {{ hx.orientation }}</span>
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
            </div>
          </t-tab-panel>

          <!-- 图库 -->
          <t-tab-panel value="media" label="楼盘图库">
            <div class="p-6">
              <div v-if="mediaLoading" class="text-center py-10"><t-loading /></div>
              <div v-else-if="!medias.length" class="text-center py-10 text-[var(--color-text-tertiary)]">暂无图片素材</div>
              <div v-else class="space-y-5">
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
            </div>
          </t-tab-panel>

          <!-- 楼盘位置 -->
          <t-tab-panel value="map" label="楼盘位置和周边配套">
            <div class="p-6">
              <div v-if="loupan.longitude && loupan.latitude" id="amap-container" class="w-full h-64 sm:h-80 lg:h-96 rounded-xl bg-gray-100"></div>
              <div v-else class="text-center py-10 text-[var(--color-text-tertiary)]">暂无位置信息</div>
              <!-- 配套设施 -->
              <div class="mt-6">
                <h3 class="text-lg font-bold text-[var(--color-text-primary)] mb-4 flex items-center gap-2">
                  <Sparkles class="w-5 h-5 text-[var(--color-primary)]" />周边配套
                </h3>
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
          </t-tab-panel>

          <!-- 一房一价 -->
          <t-tab-panel value="yfyj" label="一房一价">
            <div class="p-6">
              <div v-if="yfyjLoading" class="text-center py-10"><t-loading /></div>
              <div v-else-if="!yfyjList.length" class="text-center py-10 text-[var(--color-text-tertiary)]">暂无一房一价信息</div>
              <div v-else>
                <!-- 楼栋切换 -->
                <div class="flex flex-wrap gap-2 mb-4">
                  <t-button v-for="bd in yfyjBuildings" :key="bd" :theme="yfyjBuilding===bd?'primary':'default'" :variant="yfyjBuilding===bd?'base':'outline'" size="small" @click="yfyjBuilding=bd">{{ bd }}</t-button>
                </div>
                <!-- 模拟楼栋楼层表格 -->
                <div class="overflow-x-auto bg-white rounded-lg border border-gray-100">
                  <table class="w-full text-sm border-collapse table-fixed">
                    <thead>
                      <tr class="bg-gray-50">
                        <th class="p-2 font-medium text-center text-[var(--color-text-secondary)] w-12">楼层</th>
                        <th v-for="pos in yfyjPositions" :key="pos" class="p-2 font-medium text-center text-[var(--color-text-secondary)]">{{ pos }}室</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="floor in yfyjFloors" :key="floor" class="border-t border-gray-100">
                        <td class="p-1 text-center font-bold text-xs text-[var(--color-text-tertiary)] bg-gray-50 align-middle">{{ floor }}F</td>
                        <td v-for="pos in yfyjPositions" :key="pos" class="p-1">
                          <div v-if="yfyjGrid[floor]?.[pos]" class="rounded p-2 text-center text-xs transition-colors min-h-[72px] flex flex-col justify-center"
                               :class="{
                                 'bg-gray-50 text-gray-500': yfyjGrid[floor][pos].houseStatus===0,
                                 'bg-orange-50 text-orange-700': yfyjGrid[floor][pos].houseStatus===1,
                                 'bg-red-50 text-red-700': yfyjGrid[floor][pos].houseStatus===2,
                                 'bg-purple-50 text-purple-700': yfyjGrid[floor][pos].houseStatus===3,
                                 'bg-blue-50 text-blue-700': yfyjGrid[floor][pos].houseStatus===4
                               }">
                            <div class="font-bold mb-0.5">{{ yfyjGrid[floor][pos].roomNo }}</div>
                            <div class="opacity-80">{{ yfyjGrid[floor][pos].area }}㎡</div>
                            <div class="opacity-80" v-if="yfyjGrid[floor][pos].recordUnitPrice">{{ yfyjGrid[floor][pos].recordUnitPrice }}元/㎡</div>
                            <div class="opacity-80" v-if="yfyjGrid[floor][pos].recordTotalPrice">{{ (yfyjGrid[floor][pos].recordTotalPrice/10000).toFixed(2) }}万</div>
                            <div class="font-medium mt-0.5">{{ ['未售','认购','已售','抵押','保留'][yfyjGrid[floor][pos].houseStatus] }}</div>
                          </div>
                          <div v-else class="rounded p-1.5 text-center text-xs text-gray-300 bg-gray-50">-</div>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </t-tab-panel>
        </t-tabs>
      </div>
    </section>
  </div>

  <!-- 加载中 -->
  <div v-else class="flex justify-center py-40">
    <t-loading size="large" text="加载中..." />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Info, Building2, Shield, Car, Sparkles, MapPin, Phone, Heart, Paintbrush, Eye } from 'lucide-vue-next'
import { MessagePlugin } from 'tdesign-vue-next'
import request from '@/utils/request'

const route = useRoute()
const loupan = ref(null)
const huxings = ref([])
const medias = ref([])
const yfyjList = ref([])
const huxingLoading = ref(false)
const mediaLoading = ref(false)
const yfyjLoading = ref(false)
const activeTab = ref('info')
const yfyjBuilding = ref('')
let amapInstance = null

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

const yfyjBuildings = computed(() => [...new Set(yfyjList.value.map(i=>i.buildingNo))].sort())

// 当前楼栋的房源按楼层和位置建网格
const yfyjGrid = computed(() => {
  const grid = {}
  yfyjList.value.filter(i=>i.buildingNo===yfyjBuilding.value).forEach(item => {
    const pos = item.roomNo.replace(/^\d+/, '') // 提取房号后缀如 "01"
    if (!grid[item.floorNo]) grid[item.floorNo] = {}
    grid[item.floorNo][pos] = item
  })
  return grid
})
const yfyjFloors = computed(() => Object.keys(yfyjGrid.value).map(Number).sort((a,b)=>b-a))
const yfyjPositions = computed(() => {
  const set = new Set()
  Object.values(yfyjGrid.value).forEach(row => Object.keys(row).forEach(k=>set.add(k)))
  return [...set].sort()
})

function fmtNum(n) {
  if (!n) return '0'
  return Number(n).toLocaleString()
}

function copyTel() {
  if (loupan.value?.salesTel) {
    navigator.clipboard?.writeText(loupan.value.salesTel).then(() => {
      MessagePlugin.success('电话已复制')
    }).catch(() => {
      window.open(`tel:${loupan.value.salesTel}`)
    })
  }
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

async function fetchMedias() {
  if (medias.value.length) return
  mediaLoading.value = true
  try {
    medias.value = await request.get(`/public/loupans/${route.params.id}/medias`) || []
  } catch {} finally { mediaLoading.value = false }
}

async function fetchYfyj() {
  if (yfyjList.value.length) return
  yfyjLoading.value = true
  try {
    yfyjList.value = await request.get(`/public/loupans/${route.params.id}/yfyj`) || []
    if (yfyjBuildings.value.length) yfyjBuilding.value = yfyjBuildings.value[0]
  } catch {} finally { yfyjLoading.value = false }
}

watch(activeTab, (tab) => {
  if (tab === 'huxing') fetchHuxings()
  if (tab === 'media') fetchMedias()
  if (tab === 'map') initMap()
  if (tab === 'yfyj') fetchYfyj()
})

// 高德地图初始化
async function initMap() {
  if (amapInstance || !loupan.value?.longitude || !loupan.value?.latitude) return
  await nextTick()
  const el = document.getElementById('amap-container')
  if (!el || typeof AMap === 'undefined') {
    // AMap 脚本未加载完成，延迟重试
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

onMounted(fetchDetail)

/** 信息项渲染辅助 */
function show(val) { return val !== null && val !== undefined && val !== '' && val !== 0 }

/** 拆分配套设施文本 */
function splitItems(str) { return (str || '').split(/[,，、]/).filter(Boolean) }
</script>
