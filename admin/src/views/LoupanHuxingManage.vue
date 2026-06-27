<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">楼盘户型</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理楼盘下的户型信息</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建户型</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索户型名称" clearable class="w-[200px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-input-number v-model="filterLoupanId" placeholder="楼盘ID" :min="0" class="w-[140px]" @enter="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterLoupanId=null;search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="id" hover stripe size="small" @page-change="onPg">
        <template #huxingImage="{ row }">
          <img v-if="row.huxingImage" :src="row.huxingImage" class="w-12 h-12 object-cover rounded border" @error="e => e.target.style.display='none'" />
        </template>
        <template #floorType="{ row }">
          <t-tag size="small">{{ ['','小高层','洋房','叠墅','排屋'][row.floorType]||'未知' }}</t-tag>
        </template>
        <template #isShowHouse="{ row }"><t-tag :theme="row.isShowHouse?'success':'default'" size="small">{{ row.isShowHouse?'有':'无' }}样板间</t-tag></template>
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.id)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑户型':'新建户型'" size="450px" :footer="false">
      <t-form :data="form" label-align="top">
        <t-form-item label="楼盘ID"><t-input-number v-model="form.loupanId" :min="1" /></t-form-item>
        <t-form-item label="户型名称"><t-input v-model="form.huxingName" /></t-form-item>
        <t-form-item label="户型图">
          <div class="flex flex-col gap-2 w-full">
            <t-upload v-model="huxingFiles" :request-method="uploadHuxingImage" :max="1" accept="image/*" theme="image" @success="onHuxingSuccess" @fail="onHuxingFail" @remove="onHuxingRemove" />
            <t-input v-model="form.huxingImage" placeholder="上传后自动填入，也可手动输入URL" />
          </div>
        </t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="建筑面积(㎡)"><t-input-number v-model="form.area" :min="0" /></t-form-item>
          <t-form-item label="套内面积(㎡)"><t-input-number v-model="form.insideArea" :min="0" /></t-form-item>
        </div>
        <div class="grid grid-cols-4 gap-3">
          <t-form-item label="卧室"><t-input-number v-model="form.roomNum" :min="0" /></t-form-item>
          <t-form-item label="厅"><t-input-number v-model="form.hallNum" :min="0" /></t-form-item>
          <t-form-item label="卫生间"><t-input-number v-model="form.toiletNum" :min="0" /></t-form-item>
          <t-form-item label="阳台"><t-input-number v-model="form.balconyNum" :min="0" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="朝向"><t-input v-model="form.orientation" /></t-form-item>
          <t-form-item label="产品类型">
            <t-select v-model="form.floorType" :options="[{label:'小高层',value:1},{label:'洋房',value:2},{label:'叠墅',value:3},{label:'排屋',value:4}]" />
          </t-form-item>
        </div>
        <div class="grid grid-cols-3 gap-3">
          <t-form-item label="单价(元/㎡)"><t-input-number v-model="form.unitPrice" :min="0" /></t-form-item>
          <t-form-item label="总价起(万)"><t-input-number v-model="form.totalPriceStart" :min="0" /></t-form-item>
          <t-form-item label="总价止(万)"><t-input-number v-model="form.totalPriceEnd" :min="0" /></t-form-item>
        </div>
        <t-form-item label="是否有样板间"><t-switch v-model="form.isShowHouse" :custom-value="[1,0]" /></t-form-item>
        <t-form-item label="户型标签"><t-input v-model="form.tag" placeholder="如：三开间朝南,南北通透" /></t-form-item>
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

const initForm = () => ({ loupanId:null,huxingName:'',area:0,insideArea:0,roomNum:0,hallNum:0,toiletNum:0,balconyNum:0,orientation:'',floorType:1,unitPrice:null,totalPriceStart:null,totalPriceEnd:null,isShowHouse:0,tag:'',sort:0,huxingImage:'' })
const form = reactive(initForm())
const huxingFiles = ref([])

const cols = [
  {colKey:'id',title:'ID',width:60},
  {colKey:'loupanId',title:'楼盘ID',width:70},
  {colKey:'huxingName',title:'户型名称',width:160},
  {colKey:'huxingImage',title:'户型图',width:80},
  {colKey:'area',title:'面积(㎡)',width:80},
  {colKey:'roomNum',title:'居室',width:80},
  {colKey:'orientation',title:'朝向',width:70},
  {colKey:'floorType',title:'产品类型',width:80},
  {colKey:'unitPrice',title:'单价',width:90},
  {colKey:'isShowHouse',title:'样板间',width:80},
  {colKey:'sort',title:'排序',width:60},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterLoupanId.value)p.loupanId=filterLoupanId.value;const r=await request.get('/admin/huxings',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;huxingFiles.value=[];Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.id;huxingFiles.value=[];Object.assign(form,row);drawer.value=true}

async function uploadHuxingImage(file) {
  const fd = new FormData()
  fd.append('file', file.raw)
  const res = await request.post('/admin/medias/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  return { status: 'success', response: { url: res.url } }
}
function onHuxingSuccess({ file }) { form.huxingImage = file.response?.url || ''; MessagePlugin.success('上传成功'); huxingFiles.value = [] }
function onHuxingFail() { MessagePlugin.error('上传失败'); huxingFiles.value = [] }
function onHuxingRemove() { huxingFiles.value = [] }
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/huxings/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/huxings',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/huxings/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(fetchData)
</script>
