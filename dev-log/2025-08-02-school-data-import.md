# 导入杭州学校数据到 school 表

## 数据源
`govdata/学校数据/整理后的学校数据0802.json`，共 739 条记录。

## 操作
1. 创建 `school` 表（基于 schema.sql 定义，`govdata/学校数据/create_school.sql`）
2. 编写导入脚本 `govdata/学校数据/import_school.py`（pymysql）
3. 执行导入

## 关键处理
- **photos 字段为 list 类型**：部分记录 photos 是 JSON 数组（如 `[]`），脚本将所有 dict/list 类型转为 JSON 字符串存储
- **主键重复**：JSON 中存在 16 条重复 campus_code，使用 `INSERT ... ON DUPLICATE KEY UPDATE` 覆盖
- **经纬度**：转为 float，空值存 NULL

## 结果
- 导入 739 条，跳过 0 条
- 数据库最终 723 条（含 16 条重复被去重覆盖）
- 学校类型分布：小学 462、初中 185、九年一贯制 66、十二年一贯制 6、完全中学 4
