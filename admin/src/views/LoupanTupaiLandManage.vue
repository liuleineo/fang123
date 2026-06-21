<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">土拍地块</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理土拍地块信息</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建地块</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索地块名称/编号/竞得方" clearable class="w-[260px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #landStatus="{ row }">
          <t-tag :theme="row.landStatus===1?'success':row.landStatus===2?'danger':'default'" size="small">
            {{ row.landStatus===1?'已出让':row.landStatus===2?'流拍':'待出让' }}
          </t-tag>
        </template>
        <template #premiumRate="{ row }"><span>{{ row.premiumRate }}%</span></template>
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑地块':'新建地块'" size="500px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="地块名称"><t-input v-model="form.landName" /></t-form-item>
        <t-form-item label="宗地编号"><t-input v-model="form.landNo" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="行政区"><t-input v-model="form.district" /></t-form-item>
          <t-form-item label="板块"><t-input v-model="form.plate" /></t-form-item>
        </div>
        <t-form-item label="四至范围"><t-textarea v-model="form.landScope" :maxlength="500" /></t-form-item>
        <t-form-item label="地块状态">
          <t-select v-model="form.landStatus" :options="[{label:'待出让',value:0},{label:'已出让',value:1},{label:'流拍',value:2}]" />
        </t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="竞得方企业"><t-input v-model="form.winnerCompany" /></t-form-item>
          <t-form-item label="土地用途"><t-input v-model="form.landUseType" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="土地使用年限"><t-input-number v-model="form.landYear" :min="1" /></t-form-item>
          <t-form-item label="容积率"><t-input-number v-model="form.plotRatio" :min="0" :decimalPlaces="2" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="占地面积(㎡)"><t-input-number v-model="form.landArea" :min="0" :decimalPlaces="2" /></t-form-item>
          <t-form-item label="最大可建面积(㎡)"><t-input-number v-model="form.buildAreaLimit" :min="0" :decimalPlaces="2" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="起价(万元)"><t-input-number v-model="form.startPrice" :min="0" /></t-form-item>
          <t-form-item label="成交价(万元)"><t-input-number v-model="form.dealPrice" :min="0" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="楼面单价(元/㎡)"><t-input-number v-model="form.floorUnitPrice" :min="0" /></t-form-item>
          <t-form-item label="溢价率(%)"><t-input-number v-model="form.premiumRate" :min="0" :decimalPlaces="2" /></t-form-item>
        </div>
        <t-form-item label="成交日期">
          <t-date-picker v-model="form.dealDate" />
        </t-form-item>
        <t-form-item label="关联楼盘ID"><t-input-number v-model="form.loupanId" :min="0" /></t-form-item>
        <t-form-item label="排序"><t-input-number v-model="form.sort" :min="0" /></t-form-item>
        <t-button block theme="primary" :loading="saving" @click="save">保存</t-button>
      </t-form>
    </t-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { MessagePlugin } from 'tdesign-vue-next'
import { Plus, Search } from 'lucide-vue-next'
import request from '@/utils/request'

const drawer = ref(false); const isEdit = ref(false); const editId = ref(null); const saving = ref(false)
const data = ref([]); const loading = ref(false); const keyword = ref('')
const pg = reactive({current:1,pageSize:10,total:0})

const initForm = () => ({ landName:'',landNo:'',district:'',plate:'',landScope:'',landStatus:0,winnerCompany:'',landUseType:'',landYear:70,landArea:null,buildAreaLimit:null,plotRatio:null,startPrice:0,dealPrice:0,floorUnitPrice:0,premiumRate:0,dealDate:'',loupanId:null,sort:0 })
const form = reactive(initForm())

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'landName',title:'地块名称',width:180,ellipsis:true},
  {colKey:'landNo',title:'宗地编号',width:160},
  {colKey:'district',title:'行政区',width:80},
  {colKey:'plate',title:'板块',width:80},
  {colKey:'landStatus',title:'状态',width:80},
  {colKey:'winnerCompany',title:'竞得方',width:80},
  {colKey:'dealPrice',title:'成交价(万)',width:100},
  {colKey:'floorUnitPrice',title:'楼面价(元/㎡)',width:110},
  {colKey:'premiumRate',title:'溢价率',width:80},
  {colKey:'dealDate',title:'成交日期',width:110},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;const r=await request.get('/admin/tupai-lands',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/tupai-lands/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/tupai-lands',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/tupai-lands/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(fetchData)
</script>
