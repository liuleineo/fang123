# 学校管理 - 搜索增加按梯队筛选

## 需求
admin 学校管理列表搜索区增加按梯队（`tier`）筛选。

## 实现
### 后端
`controller/SchoolController.java`：
- admin 列表接口 `/api/admin/schools` 新增 `tier` 参数（Integer），筛选 `w.eq(School::getTier, tier)`

### admin 前端
`views/SchoolManage.vue`：
- 搜索区新增"梯队"下拉（tierOpts：1/2/3梯队）
- 新增 `filterTier` 状态
- fetchData 传递 `tier` 参数
- 重置按钮同时清空梯队筛选

## 说明
- 梯队已存在 tierOpts（之前表单用过），复用
- 支持按 1/2/3 梯队精确筛选
