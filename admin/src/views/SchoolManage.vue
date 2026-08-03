<template>
  <div>
    <div class="mb-6 flex items-center justify-between">
      <div><h1 class="text-2xl font-bold">学校</h1><p class="text-sm text-[var(--color-text-tertiary)] mt-1">管理学校校区信息</p></div>
      <t-button theme="primary" @click="openCreate"><Plus class="w-4 h-4 mr-1" />新建学校</t-button>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 overflow-hidden">
      <div class="flex gap-3 items-center p-4 border-b border-gray-50">
        <t-input v-model="keyword" placeholder="搜索学校名称/校区名称/校区码/小区" clearable class="w-[280px]" @enter="search" @clear="search">
          <template #prefix-icon><Search class="w-4 h-4" /></template>
        </t-input>
        <t-select v-model="filterType" placeholder="学校类型" clearable class="w-[140px]" :options="typeOpts" @change="search" />
        <t-button theme="primary" @click="search"><Search class="w-4 h-4 mr-1" />搜索</t-button>
        <t-button variant="outline" @click="keyword='';filterType='';search()">重置</t-button>
      </div>
      <t-table :data="data" :columns="cols" :loading="loading" :pagination="pg" row-key="campusCode" hover stripe size="small" @page-change="onPg">
        <template #schoolType="{ row }"><t-tag size="small">{{ row.schoolType||'-' }}</t-tag></template>
        <template #tier="{ row }">
          <t-tag v-if="row.tier" size="small" theme="warning">{{ row.tier }}梯队</t-tag>
          <span v-else class="text-xs text-gray-300">-</span>
        </template>
        <template #schoolLogo="{ row }">
          <t-image v-if="row.schoolLogo" :src="row.schoolLogo" fit="cover" class="w-10 h-10 rounded" />
          <span v-else class="text-xs text-gray-300">无</span>
        </template>
        <template #createTime="{ row }"><span class="text-xs text-[var(--color-text-tertiary)]">{{ fmt(row.createTime) }}</span></template>
        <template #operation="{ row }">
          <t-space size="small">
            <t-button variant="text" theme="primary" size="small" @click="openEdit(row)">编辑</t-button>
            <t-popconfirm content="确定删除？" @confirm="del(row.campusCode)"><t-button variant="text" theme="danger" size="small">删除</t-button></t-popconfirm>
          </t-space>
        </template>
      </t-table>
    </div>

    <t-drawer v-model:visible="drawer" :header="isEdit?'编辑学校':'新建学校'" size="640px" :footer="false">
      <t-form :data="form" label-align="top">
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="校区标识码（唯一）"><t-input v-model="form.campusCode" :disabled="isEdit" placeholder="如：XX001" /></t-form-item>
          <t-form-item label="学校机构标识码"><t-input v-model="form.schoolOrgCode" /></t-form-item>
        </div>
        <t-form-item label="学校机构名称"><t-input v-model="form.schoolOrgName" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="校区名称"><t-input v-model="form.campusName" /></t-form-item>
          <t-form-item label="学校类型"><t-select v-model="form.schoolType" :options="typeOpts" clearable /></t-form-item>
        </div>
        <t-form-item label="学校地址"><t-input v-model="form.schoolAddress" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="联系电话"><t-input v-model="form.contactPhone" /></t-form-item>
          <t-form-item label="举办者类型"><t-input v-model="form.sponsorType" placeholder="非民办/民办" /></t-form-item>
        </div>
        <div class="grid grid-cols-2 gap-3">
          
          <t-form-item label="经度"><t-input-number v-model="form.longitude" :decimal-places="7" /></t-form-item>
          <t-form-item label="纬度"><t-input-number v-model="form.latitude" :decimal-places="7" /></t-form-item>
          <t-form-item label="梯队"><t-select v-model="form.tier" clearable placeholder="选择梯队" :options="tierOpts" /></t-form-item>
        </div>
        <t-form-item label="教育行政主管部门"><t-input v-model="form.eduAdminDepartment" /></t-form-item>
        <t-form-item label="学区范围"><t-textarea v-model="form.schoolDistrictScope" :autosize="{minRows:2,maxRows:5}" /></t-form-item>
        <t-form-item label="学校简介"><t-textarea v-model="form.schoolIntro" :autosize="{minRows:2,maxRows:5}" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="对口初中名称"><t-input v-model="form.targetMiddleSchoolName" /></t-form-item>
          <t-form-item label="对口初中编码"><t-input v-model="form.targetMiddleSchoolCode" /></t-form-item>
        </div>
        <t-form-item label="小区名称（逗号分隔）"><t-textarea v-model="form.communityNames" :autosize="{minRows:2,maxRows:4}" /></t-form-item>
        <div class="grid grid-cols-2 gap-3">
          <t-form-item label="学区地图图片URL"><t-input v-model="form.districtMapImage" /></t-form-item>
          <t-form-item label="学校Logo URL"><t-input v-model="form.schoolLogo" /></t-form-item>
        </div>
        <t-form-item label="地图围栏坐标（lng,lat;lng,lat）"><t-input v-model="form.mapFence" /></t-form-item>
        <t-form-item label="照片URL列表（逗号分隔）"><t-input v-model="form.photos" /></t-form-item>
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
const data = ref([]); const loading = ref(false); const keyword = ref(''); const filterType = ref('')
const pg = reactive({current:1,pageSize:10,total:0})

const typeOpts = [
  { label: '小学', value: '小学' }, { label: '初中', value: '初中' }, { label: '九年一贯制', value: '九年一贯制' }
]
const tierOpts = [
  { label: '1梯队', value: 1 }, { label: '2梯队', value: 2 }, { label: '3梯队', value: 3 }
]

const initForm = () => ({
  campusCode:'', schoolOrgCode:'', schoolOrgName:'', campusName:'', schoolAddress:'', contactPhone:'',
  schoolType:'', sponsorType:'', tier:null, longitude:null, latitude:null, eduAdminDepartment:'',
  schoolDistrictScope:'', schoolIntro:'', targetMiddleSchoolName:'', targetMiddleSchoolCode:'',
  communityNames:'', districtMapImage:'', mapFence:'', photos:'', schoolLogo:''
})
const form = reactive(initForm())

const cols = [
  {colKey:'campusCode',title:'校区码',width:80},
  {colKey:'schoolOrgName',title:'学校名称',width:180,ellipsis:true},
  {colKey:'campusName',title:'校区名称',width:120,ellipsis:true},
  {colKey:'schoolType',title:'类型',width:100},
  {colKey:'tier',title:'梯队',width:80},
  {colKey:'schoolAddress',title:'地址',width:180,ellipsis:true},
  {colKey:'schoolLogo',title:'Logo',width:60},
  {colKey:'targetMiddleSchoolName',title:'对口初中',width:120,ellipsis:true},
  {colKey:'createTime',title:'创建时间',width:160},
  {colKey:'operation',title:'操作',width:120,fixed:'right'},
]

function fmt(t){if(!t)return'';const d=new Date(t);return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`}

async function fetchData() {
  loading.value=true
  try{const p={page:pg.current,size:pg.pageSize};if(keyword.value)p.keyword=keyword.value;if(filterType.value)p.schoolType=filterType.value;const r=await request.get('/admin/schools',{params:p});data.value=r.records||[];pg.total=r.total||0}catch(e){}finally{loading.value=false}
}
function search(){pg.current=1;fetchData()}
function onPg(p){pg.current=p.current;pg.pageSize=p.pageSize;fetchData()}
function openCreate(){isEdit.value=false;editId.value=null;Object.assign(form,initForm());drawer.value=true}
function openEdit(row){isEdit.value=true;editId.value=row.campusCode;Object.assign(form,row);drawer.value=true}
async function save(){
  saving.value=true
  try{if(isEdit.value){await request.put(`/admin/schools/${editId.value}`,form);MessagePlugin.success('已更新')}else{await request.post('/admin/schools',form);MessagePlugin.success('已创建')}drawer.value=false;fetchData()}catch(e){MessagePlugin.error(e.response?.data?.msg||'保存失败')}finally{saving.value=false}
}
async function del(id){await request.delete(`/admin/schools/${id}`);MessagePlugin.success('已删除');fetchData()}

onMounted(fetchData)
</script>
