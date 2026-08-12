# 学校管理 - 搜索增加按行政区筛选

## 需求
admin 学校管理列表搜索区增加按行政区（`edu_admin_department`）筛选。

## 实现
### 后端
`controller/SchoolController.java` 新增接口：
- `GET /api/admin/schools/departments` - 返回去重的行政区列表（groupBy + 过滤空值）

### admin 前端
`views/SchoolManage.vue`：
- 搜索区新增"行政区"下拉（`t-select`，options 来自 departments 接口）
- 新增 `filterDept` 状态和 `deptOpts` 选项
- fetchData 传递 `eduAdminDepartment` 参数
- `fetchDepts()` 加载行政区选项，onMounted 时调用
- 重置按钮同时清空行政区筛选

## 说明
- 行政区选项通过专用接口去重获取
- 支持按行政区精确筛选
