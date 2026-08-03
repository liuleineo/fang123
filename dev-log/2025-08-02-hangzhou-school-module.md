# 新增杭州学校模块（后端 CRUD + admin 端）

## 数据库
`hz_school` 表已存在（schema.sql 已定义），主键为 `campus_code`。

## 后端新增文件
- `entity/School.java` - 实体类，主键 campusCode（INPUT 手动赋值），表名 school
- `mapper/SchoolMapper.java` - MyBatis-Plus Mapper
- `service/SchoolService.java` - Service 接口
- `service/impl/SchoolServiceImpl.java` - Service 实现
- `controller/SchoolController.java` - CRUD 接口

> 注：类名/文件名已去掉 Hangzhou 前缀，统一为 School。

### 接口列表
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/schools` | 分页+搜索（keyword/schoolType/eduAdminDepartment） |
| GET | `/api/admin/schools/{campusCode}` | 详情 |
| POST | `/api/admin/schools` | 新增（校验 campusCode 唯一） |
| PUT | `/api/admin/schools/{campusCode}` | 更新 |
| DELETE | `/api/admin/schools/{campusCode}` | 删除 |
| POST | `/api/admin/schools/upload` | 上传图片到 COS |

## admin 端新增
- `views/SchoolManage.vue` - 学校管理页面（列表+搜索+抽屉表单）
- `router/index.js` - 新增 `/schools` 路由
- `layouts/AdminLayout.vue` - 菜单新增"杭州学校"，图标 GraduationCap

## 说明
- 主键 campusCode 为字符串，编辑时禁用修改
- 支持按学校类型筛选（小学/初中/九年一贯制）
- 图片上传接口已预留（schoolLogo/districtMapImage/photos 用 URL 字段存储）
