# 学校模块新增"梯队"字段

## 需求
school 表增加"梯队"字段（如 1梯队、2梯队），可为空。

## 修改内容

### 数据库
- `schema.sql`：`school` 表新增 `tier TINYINT DEFAULT NULL COMMENT '梯队：1梯队/2梯队，可为空'`
- 实际数据库已执行 `ALTER TABLE school ADD COLUMN tier TINYINT DEFAULT NULL`

### 后端
- `entity/School.java`：新增 `private Integer tier;`

### admin 端
- `views/SchoolManage.vue`：
  - 列表新增"梯队"列（t-tag warning 显示，空值显示 -）
  - 表单新增"梯队"下拉选择（1/2/3梯队，可清空）
  - initForm 增加 tier 字段

### web 端学校地图
- `views/SchoolMap.vue`：
  - 筛选栏新增"梯队"下拉（1/2/3梯队）
  - fetchData 传递 tier 参数
  - reset 重置梯队

### 后端公开接口
- `SchoolController.java`：`/api/public/schools` 新增 `tier` 筛选参数（Integer）
