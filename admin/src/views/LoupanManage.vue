<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">楼盘管理</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理楼盘主信息</p></div>
      <div class="flex gap-2">
        <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建楼盘</t-button>
        <t-button theme="warning" variant="outline" @click="openAiDialog"><Sparkles class="w-4 h-4 mr-1" />AI新增楼盘</t-button>
      </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索楼盘名称/行政区/板块/开发商" clearable class="w-[260px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #salesStatus="{ row }">
          <t-tag :theme="row.salesStatus===1?'success':row.salesStatus===2?'default':row.salesStatus===3?'warning':row.salesStatus===4?'primary':'default'" size="small">
            {{ ['待开放','在售','售罄','停工','交付'][row.salesStatus]||'未知' }}
          </t-tag>
        </template>
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑楼盘':'新建楼盘'" size="600px" :footer="false">
      <t-tabs v-model="activeTab" defaultValue="basic">
        <t-tab-panel value="basic" label="基本信息">
          <t-form :data="form" label-align="top">
            <t-form-item label="楼盘名称"><t-input v-model="form.projectName" /></t-form-item>
            <t-form-item label="楼盘简称"><t-input v-model="form.shortName" /></t-form-item>
            <t-form-item label="封面图">
              <div class="flex flex-col gap-2 w-full">
                <t-upload v-model="coverFiles" :request-method="uploadCover" :max="1" accept="image/*" theme="image" @success="onCoverSuccess" @fail="onCoverFail" @remove="onCoverRemove" />
                <t-input v-model="form.coverImage" placeholder="上传后自动填入，也可手动输入URL" />
              </div>
            </t-form-item>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="行政区"><t-input v-model="form.district" /></t-form-item>
              <t-form-item label="板块"><t-input v-model="form.plate" /></t-form-item>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="经度"><t-input v-model="form.longitude" placeholder="如 120.345678" /></t-form-item>
              <t-form-item label="纬度"><t-input v-model="form.latitude" placeholder="如 30.123456" /></t-form-item>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="售楼部位置"><t-input v-model="form.salesAddress" /></t-form-item>
              <t-form-item label="楼盘地址"><t-input v-model="form.projectAddress" /></t-form-item>
            </div>
            <t-form-item label="样板房说明"><t-textarea v-model="form.showHouseDesc" :maxlength="500" /></t-form-item>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="售楼状态">
                <t-select v-model="form.salesStatus" :options="[{label:'待开放',value:0},{label:'在售',value:1},{label:'售罄',value:2},{label:'停工',value:3},{label:'交付',value:4}]" />
              </t-form-item>
              <t-form-item label="售楼电话"><t-input v-model="form.salesTel" /></t-form-item>
            </div>
            <t-form-item label="户型面积范围">
              <div class="flex gap-3 items-center">
                <t-input-number v-model="form.areaMin" :min="0" placeholder="最小㎡" />
                <span class="text-[var(--color-text-tertiary)]">—</span>
                <t-input-number v-model="form.areaMax" :min="0" placeholder="最大㎡" />
              </div>
            </t-form-item>
          </t-form>
        </t-tab-panel>
        <t-tab-panel value="sales" label="价格/销售">
          <t-form :data="form" label-align="top">
            <div class="grid grid-cols-3 gap-3">
              <t-form-item label="均价(元/㎡)"><t-input-number v-model="form.avgUnitPrice" :min="0" /></t-form-item>
              <t-form-item label="最低总价(万)"><t-input-number v-model="form.minTotalPrice" :min="0" /></t-form-item>
              <t-form-item label="最高总价(万)"><t-input-number v-model="form.maxTotalPrice" :min="0" /></t-form-item>
            </div>
            <t-form-item label="价格标签"><t-input v-model="form.priceTag" placeholder="如：改善,精装住宅" /></t-form-item>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="装修类型">
                <t-select v-model="form.decorateType" :options="[{label:'精装',value:1},{label:'毛坯',value:2},{label:'简装',value:3}]" />
              </t-form-item>
              <t-form-item label="楼盘类型">
                <t-select v-model="form.houseType" :options="[{label:'住宅',value:1},{label:'公寓',value:2},{label:'商铺',value:3},{label:'别墅',value:4}]" />
              </t-form-item>
            </div>
            <t-form-item label="产权年限"><t-input-number v-model="form.propertyRightYear" :min="1" /></t-form-item>
          </t-form>
        </t-tab-panel>
        <t-tab-panel value="build" label="建筑指标">
          <t-form :data="form" label-align="top">
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="楼栋总数"><t-input-number v-model="form.buildingTotal" :min="0" /></t-form-item>
              <t-form-item label="总户数"><t-input-number v-model="form.houseTotal" :min="0" /></t-form-item>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="最低楼层"><t-input-number v-model="form.floorMin" :min="0" /></t-form-item>
              <t-form-item label="最高楼层"><t-input-number v-model="form.floorMax" :min="0" /></t-form-item>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="最低层高(m)"><t-input-number v-model="form.floorHeightMin" :min="0" :decimalPlaces="1" /></t-form-item>
              <t-form-item label="最高层高(m)"><t-input-number v-model="form.floorHeightMax" :min="0" :decimalPlaces="1" /></t-form-item>
            </div>
            <div class="grid grid-cols-3 gap-3">
              <t-form-item label="总建面(㎡)"><t-input-number v-model="form.buildArea" :min="0" /></t-form-item>
              <t-form-item label="占地面积(㎡)"><t-input-number v-model="form.landArea" :min="0" /></t-form-item>
              <t-form-item label="容积率"><t-input-number v-model="form.plotRatio" :min="0" :decimalPlaces="2" /></t-form-item>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="绿地率(%)"><t-input-number v-model="form.greenRate" :min="0" :decimalPlaces="2" /></t-form-item>
              <t-form-item label="自持率(%)"><t-input-number v-model="form.selfHoldRate" :min="0" :decimalPlaces="2" /></t-form-item>
            </div>
            <t-form-item label="外立面材料"><t-textarea v-model="form.facadeMaterial" /></t-form-item>
            <t-form-item label="交房时间"><t-date-picker v-model="form.deliveryDate" /></t-form-item>
          </t-form>
        </t-tab-panel>
        <t-tab-panel value="dev" label="开发信息">
          <t-form :data="form" label-align="top">
            <t-form-item label="开发公司"><t-input v-model="form.projectCompany" /></t-form-item>
            <t-form-item label="开发品牌"><t-input v-model="form.brandList" /></t-form-item>
            <div class="grid grid-cols-3 gap-3">
              <t-form-item label="拿地总价(万)"><t-input-number v-model="form.landPrice" :min="0" /></t-form-item>
              <t-form-item label="楼面价(元/㎡)"><t-input-number v-model="form.landUnitPrice" :min="0" /></t-form-item>
              <t-form-item label="拿地日期"><t-date-picker v-model="form.landBuyDate" /></t-form-item>
            </div>
            <t-form-item label="关联土拍地块ID"><t-input-number v-model="form.landId" :min="0" /></t-form-item>
            <t-form-item label="物业公司"><t-input v-model="form.propertyCompany" /></t-form-item>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="小高/洋房物业费"><t-input-number v-model="form.propertyFeeHigh" :min="0" :decimalPlaces="2" /></t-form-item>
              <t-form-item label="排屋别墅物业费"><t-input-number v-model="form.propertyFeeVilla" :min="0" :decimalPlaces="2" /></t-form-item>
            </div>
            <div class="grid grid-cols-3 gap-3">
              <t-form-item label="总车位"><t-input-number v-model="form.parkTotal" :min="0" /></t-form-item>
              <t-form-item label="可售车位"><t-input-number v-model="form.parkSellNum" :min="0" /></t-form-item>
              <t-form-item label="车位配比"><t-input v-model="form.parkRatio" /></t-form-item>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <t-form-item label="是否人车分流">
                <t-select v-model="form.peopleCarSeparate" :options="[{label:'是',value:1},{label:'否',value:0}]" />
              </t-form-item>
            </div>
          </t-form>
        </t-tab-panel>
        <t-tab-panel value="support" label="配套设施">
          <t-form :data="form" label-align="top">
            <t-form-item label="教育配套"><t-textarea v-model="form.eduSupport" :autosize="{minRows:2}" /></t-form-item>
            <t-form-item label="交通配套"><t-textarea v-model="form.trafficSupport" :autosize="{minRows:2}" /></t-form-item>
            <t-form-item label="医疗配套"><t-textarea v-model="form.medicalSupport" :autosize="{minRows:2}" /></t-form-item>
            <t-form-item label="商业配套"><t-textarea v-model="form.businessSupport" :autosize="{minRows:2}" /></t-form-item>
            <t-form-item label="景观配套"><t-textarea v-model="form.viewSupport" :autosize="{minRows:2}" /></t-form-item>
            <t-form-item label="小区配套"><t-textarea v-model="form.communityFacility" :autosize="{minRows:2}" /></t-form-item>
            <t-form-item label="排序"><t-input-number v-model="form.sort" :min="0" /></t-form-item>
          </t-form>
        </t-tab-panel>
      </t-tabs>
      <t-button block theme="primary" :loading="saving" @click="save" class="mt-4">保存</t-button>
    </t-drawer>

    <!-- AI 新增楼盘对话框 -->
    <t-dialog v-model:visible="aiVisible" header="AI 新增楼盘" width="640px" :footer="false" :close-on-overlay-click="false">
      <div class="space-y-4">
        <t-alert theme="info" message="上传楼盘资料图片（如宣传海报、户型图、规划图等），AI 将自动识别并提取楼盘信息。" />
        
        <!-- 图片上传区 -->
        <t-upload 
          v-model="aiFiles" 
          :request-method="aiUploadMethod" 
          :max="5" 
          multiple 
          accept="image/*" 
          theme="image" 
          :auto-upload="false"
          tips="支持 JPG/PNG/WebP，单张不超过 5MB，最多 5 张"
        />
        
        <!-- 识别按钮 -->
        <div class="flex justify-center">
          <t-button theme="primary" size="large" :loading="aiParsing" @click="startAiParse" :disabled="aiFiles.length === 0">
            <Sparkles class="w-4 h-4 mr-1" />{{ aiParsing ? 'AI 识别中...' : '开始识别' }}
          </t-button>
        </div>
        
        <!-- 解析结果 -->
        <div v-if="aiResult" class="border rounded-lg p-4 bg-gray-50 max-h-[400px] overflow-y-auto">
          <h3 class="font-semibold mb-3 text-base">识别结果</h3>
          
          <!-- OCR 原始文本（折叠） -->
          <t-collapse class="mb-3">
            <t-collapse-panel header="OCR 原始识别文本">
              <pre class="text-xs text-gray-600 whitespace-pre-wrap">{{ aiResult.ocrText }}</pre>
            </t-collapse-panel>
          </t-collapse>
          
          <!-- 结构化字段 -->
          <div class="grid grid-cols-2 gap-2 text-sm">
            <template v-for="(label, key) in fieldLabels" :key="key">
              <div v-if="aiResult.fields[key] !== null && aiResult.fields[key] !== undefined && aiResult.fields[key] !== ''" class="flex gap-1">
                <span class="text-gray-500 shrink-0">{{ label }}：</span>
                <span class="font-medium">{{ formatFieldValue(key, aiResult.fields[key]) }}</span>
              </div>
            </template>
            <div v-if="!hasAnyField" class="col-span-2 text-gray-400 text-center py-4">
              未识别到有效字段，请检查图片是否清晰，或尝试更换图片
            </div>
          </div>
        </div>
        
        <!-- 底部操作 -->
        <div v-if="aiResult && hasAnyField" class="flex justify-end gap-2 pt-2 border-t">
          <t-button variant="outline" @click="aiVisible = false">关闭</t-button>
          <t-button theme="primary" @click="fillFormFromAi">
            <Check class="w-4 h-4 mr-1" />确认并填入表单
          </t-button>
        </div>
        <div v-else-if="aiResult && !hasAnyField" class="flex justify-end gap-2 pt-2 border-t">
          <t-button variant="outline" @click="aiResult=null;aiFiles=[]">重新上传</t-button>
          <t-button variant="outline" @click="aiVisible = false">关闭</t-button>
        </div>
      </div>
    </t-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search, Sparkles, Check } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref(''); const activeTab = ref('basic')
const coverFiles = ref([])
const pg = reactive({current:1,pageSize:10,total:0})

// ===== AI 新增楼盘相关 =====
const aiVisible = ref(false)
const aiFiles = ref([])
const aiParsing = ref(false)
const aiResult = ref(null)

const fieldLabels = {
  projectName:'楼盘名称', shortName:'楼盘简称', district:'行政区', plate:'板块',
  avgUnitPrice:'均价(元/㎡)', areaMin:'最小面积(㎡)', areaMax:'最大面积(㎡)',
  buildArea:'总建面(㎡)', landArea:'占地面积(㎡)', projectCompany:'开发公司',
  brandList:'开发品牌', projectAddress:'楼盘地址', salesAddress:'售楼部位置',
  houseType:'楼盘类型', decorateType:'装修类型', buildingTotal:'楼栋总数',
  houseTotal:'总户数', greenRate:'绿地率(%)', plotRatio:'容积率',
  propertyCompany:'物业公司', deliveryDate:'交房时间', salesTel:'售楼电话',
  propertyRightYear:'产权年限', minTotalPrice:'最低总价(万)', maxTotalPrice:'最高总价(万)',
  priceTag:'价格标签', landPrice:'拿地总价(万)', landUnitPrice:'楼面价(元/㎡)',
  propertyFeeHigh:'小高/洋房物业费', propertyFeeVilla:'别墅物业费',
  parkTotal:'总车位', parkSellNum:'可售车位', parkRatio:'车位配比',
  facadeMaterial:'外立面材料', selfHoldRate:'自持率(%)',
  floorHeightMin:'最低层高(m)', floorHeightMax:'最高层高(m)',
  floorMin:'最低楼层', floorMax:'最高楼层', longitude:'经度', latitude:'纬度',
  eduSupport:'教育配套', trafficSupport:'交通配套', medicalSupport:'医疗配套',
  businessSupport:'商业配套', viewSupport:'景观配套', communityFacility:'小区配套',
  showHouseDesc:'样板房说明'
}

const houseTypeLabels = {1:'住宅',2:'公寓',3:'商铺',4:'别墅'}
const decorateTypeLabels = {1:'精装',2:'毛坯',3:'简装'}

const hasAnyField = computed(() => {
  if (!aiResult.value?.fields) return false
  const f = aiResult.value.fields
  return Object.keys(fieldLabels).some(k => f[k] !== null && f[k] !== undefined && f[k] !== '')
})

function formatFieldValue(key, val) {
  if (val === null || val === undefined) return '-'
  if (key === 'houseType') return houseTypeLabels[val] || val
  if (key === 'decorateType') return decorateTypeLabels[val] || val
  return val
}

function openAiDialog() {
  aiFiles.value = []
  aiResult.value = null
  aiVisible.value = true
}

function aiUploadMethod() {
  // 不自动上传，由 startAiParse 手动提交
  return Promise.resolve({ status: 'success', response: {} })
}

async function startAiParse() {
  if (aiFiles.value.length === 0) {
    MessagePlugin.warning('请先上传图片')
    return
  }
  aiParsing.value = true
  aiResult.value = null
  try {
    const fd = new FormData()
    aiFiles.value.forEach((f) => {
      fd.append('files', f.raw)
    })
    const res = await request.post('/admin/loupans/ai-parse', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
    aiResult.value = res.data || res
    if (!hasAnyField.value) {
      MessagePlugin.warning('AI 未能识别出有效字段，请检查图片质量或更换图片')
    } else {
      MessagePlugin.success(`识别成功，提取到 ${Object.keys(fieldLabels).filter(k => aiResult.value.fields[k] !== null && aiResult.value.fields[k] !== undefined && aiResult.value.fields[k] !== '').length} 个字段`)
    }
  } catch (e) {
    MessagePlugin.error('AI 识别失败：' + (e.response?.data?.msg || e.message || '网络错误'))
  } finally {
    aiParsing.value = false
  }
}

function fillFormFromAi() {
  if (!aiResult.value?.fields) return
  const f = aiResult.value.fields
  
  // 打开新建楼盘抽屉
  isEdit.value = false; editId.value = null; coverFiles.value = []
  Object.assign(form, initForm())
  
  // 将 AI 解析结果填入 form（只填非 null 的字段）
  Object.keys(fieldLabels).forEach(k => {
    if (f[k] !== null && f[k] !== undefined && f[k] !== '') {
      form[k] = f[k]
    }
  })
  
  // 如果 AI 识别到了图片 URL，自动填入封面图
  if (aiResult.value.imageUrls?.length > 0 && !form.coverImage) {
    form.coverImage = aiResult.value.imageUrls[0]
  }
  
  aiVisible.value = false
  activeTab.value = 'basic'
  drawer.value = true
  MessagePlugin.success('已自动填入 AI 识别的字段，请核对后保存')
}

const initForm = () => ({
  coverImage:'',projectName:'',shortName:'',district:'',plate:'',longitude:'',latitude:'',
  avgUnitPrice:null,minTotalPrice:null,maxTotalPrice:null,priceTag:'',
  salesAddress:'',salesStatus:0,salesTel:'',projectAddress:'',showHouseDesc:'',
  deliveryDate:'',floorHeightMin:null,floorHeightMax:null,buildingTotal:0,floorMin:0,floorMax:0,
  areaMin:0,areaMax:0,decorateType:1,propertyRightYear:70,houseType:1,
  communityFacility:'',peopleCarSeparate:1,propertyFeeHigh:null,propertyFeeVilla:null,
  propertyCompany:'',parkTotal:0,parkSellNum:0,parkRatio:'',facadeMaterial:'',selfHoldRate:0,
  buildArea:0,landArea:0,houseTotal:0,plotRatio:0,greenRate:0,projectCompany:'',brandList:'',
  landPrice:0,landUnitPrice:0,landBuyDate:'',
  eduSupport:'',trafficSupport:'',medicalSupport:'',businessSupport:'',viewSupport:'',sort:0,landId:null
})
const form = reactive(initForm())

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'projectName',title:'楼盘名称',width:140,ellipsis:true},
  {colKey:'district',title:'行政区',width:80},
  {colKey:'plate',title:'板块',width:80},
  {colKey:'salesStatus',title:'状态',width:80},
  {colKey:'avgUnitPrice',title:'均价',width:90},
  {colKey:'houseType',title:'类型',width:70},
  {colKey:'buildingTotal',title:'楼栋',width:60},
  {colKey:'houseTotal',title:'户数',width:60},
  {colKey:'projectCompany',title:'开发商',width:120,ellipsis:true},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;const r=await request.get('/admin/loupans',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;coverFiles.value=[];Object.assign(form,initForm());activeTab.value='basic';drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;coverFiles.value=[];Object.assign(form,row);activeTab.value='basic';drawer.value=true}

async function uploadCover(file) {
  const fd = new FormData()
  fd.append('file', file.raw)
  const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  return { status: 'success', response: { url: res.url } }
}
function onCoverSuccess({ file }) { form.coverImage = file.response?.url || ''; MessagePlugin.success('封面上传成功'); coverFiles.value = [] }
function onCoverFail() { MessagePlugin.error('上传失败'); coverFiles.value = [] }
function onCoverRemove() { coverFiles.value = [] }
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/loupans/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/loupans',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/loupans/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(fetchData)
</script>
