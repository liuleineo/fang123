# 学校管理 - 增加"更新数据"按钮（刷新小区）

## 需求
在 admin 学校管理列表操作列增加"更新数据"按钮，点击后调用入学早知道 API 获取学校数据，解析 `result.appSchoolDistrictInfoEntityList` 中所有 `xqmc` 拼接成逗号分隔字符串，更新 `community_names` 字段。

## 实现
### 后端
`controller/SchoolController.java` 新增接口：
- `POST /api/admin/schools/{campusCode}/refresh-community`
- 调用 `https://rxyj.hzedu.gov.cn/hzjyAppServer/api/AppSchoolInfo/getSchoolInfo?year=2026&schoolName={campusCode}`
- 解析 JSON 的 `result.appSchoolDistrictInfoEntityList[].xqmc`，去重后用逗号拼接
- 更新 `community_names` 字段，返回小区数量和结果
- 注入 `ObjectMapper` 解析 JSON

### admin 前端
`views/SchoolManage.vue`：
- 操作列新增"更新数据"按钮（带 loading 状态）
- `updateData(row)` 调用后端刷新接口，成功后提示并刷新列表

## 说明
- 通过后端代理调用 rxyj API，避免前端跨域问题
- xqmc 去重拼接
