# 学校数据 SQL 导入（整理后的学校数据0802.json）

## 需求
把 `govdata/学校数据/整理后的学校数据0802.json`（753 条）转成可导入 `school` 表的 SQL 语句。

## 生成脚本
`govdata/学校数据/generate_school_sql.py`
- 读取 JSON，生成 INSERT SQL
- 处理特殊字段：
  - `photos` 可能是 list/str → 转 JSON 字符串
  - `longitude`/`latitude` 是 str → 转数字，无效值置 NULL
  - 转义单引号/反斜杠/换行
- 使用 `INSERT ... ON DUPLICATE KEY UPDATE`，campus_code 重复时覆盖更新

## 输出
`govdata/学校数据/import_school.sql`（753 条 INSERT）

## 执行结果
- SQL 语法验证通过，无错误
- 数据库 school 表现有 737 条（含重复 campus_code 覆盖合并）
- tier 字段不在 JSON 中，导入后为 NULL
