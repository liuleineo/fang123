<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">一房一价</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理每套房源的备案价和销售价</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建房源</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索楼栋号/房号" clearable class="w-[200px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-input-number v-model="filterLoupanId" placeholder="楼盘ID" :min="0" class="w-[140px]" @enter="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId=null;search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #houseStatus="{ row }">
          <t-tag :theme="row.houseStatus===0?'default':row.houseStatus===1?'warning':row.houseStatus===2?'success':row.houseStatus===3?'danger':'primary'" size="small">
            {{ ['未售','认购','已售','抵押','保留'][row.houseStatus]||'未知' }}
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

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑房源':'新建房源'" size="450px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="户型ID"><t-input-number v-model="form.huxingId" :min="0" /></t-form-item>
        <div class="grid grid-cols-3 gap-3">
          <t-form-item label="楼栋号"><t-input v-model="form.buildingNo" /></t-form-item>
          <t-form-item label="楼层"><t-input-number v-model="form.floorNo" :min="1" /></t-form-item>
          <t-form-item label="房号"><t-input v-model="form.roomNo" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="建筑面积(㎡)"><t-input-number v-model="form.area" :min="0" :decimalPlaces="2" /></t-form-item>
          <t-form-item label="朝向"><t-input v-model="form.orientation" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="备案单价(元/㎡)"><t-input-number v-model="form.recordUnitPrice" :min="0" /></t-form-item>
          <t-form-item label="备案总价(元)"><t-input-number v-model="form.recordTotalPrice" :min="0" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="销售单价(元/㎡)"><t-input-number v-model="form.saleUnitPrice" :min="0" /></t-form-item>
          <t-form-item label="销售总价(元)"><t-input-number v-model="form.saleTotalPrice" :min="0" /></t-form-item>
        </div>
        <t-form-item label="房源状态">
          <t-select v-model="form.houseStatus" :options="[{label:'未售',value:0},{label:'认购',value:1},{label:'已售',value:2},{label:'抵押',value:3},{label:'保留',value:4}]" />
        </t-form-item>
        <t-form-item label="备注"><t-textarea v-model="form.remark" :maxlength="500" /></t-form-item>
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
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterLoupanId = ref(null)
const pg = reactive({current:1,pageSize:10,total:0})

const initForm = () => ({ loupanId:null,huxingId:null,buildingNo:'',floorNo:1,roomNo:'',area:0,recordUnitPrice:0,recordTotalPrice:0,saleUnitPrice:null,saleTotalPrice:null,houseStatus:0,orientation:'',remark:'',sort:0 })
const form = reactive(initForm())

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'loupanId',title:'楼盘ID',width:70},
  {colKey:'huxingId',title:'户型ID',width:70},
  {colKey:'buildingNo',title:'楼栋',width:80},
  {colKey:'floorNo',title:'楼层',width:60},
  {colKey:'roomNo',title:'房号',width:80},
  {colKey:'area',title:'面积(㎡)',width:80},
  {colKey:'recordUnitPrice',title:'备案单价',width:90},
  {colKey:'recordTotalPrice',title:'备案总价',width:100},
  {colKey:'houseStatus',title:'状态',width:70},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterLoupanId.value)p.loupanId=filterLoupanId.value;const r=await request.get('/admin/yfyj',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/yfyj/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/yfyj',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/yfyj/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(fetchData)
</script>
