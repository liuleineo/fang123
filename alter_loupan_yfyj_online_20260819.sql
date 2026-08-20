-- ============================================================
-- 线上数据库 ALTER 脚本：loupan_yfyj 表结构同步（2026-08-19）
-- 数据库：fang123（线上）
-- 说明：本脚本汇总了近期对 loupan_yfyj 表的全部结构变更。
-- 执行前建议备份：mysqldump fang123 loupan_yfyj > loupan_yfyj_bak.sql
-- ============================================================

USE fang123;

-- ------------------------------------------------------------
-- 1. 新增字段：预售许可证编号、房屋编码（若不存在）
--    可先执行确认：SHOW COLUMNS FROM loupan_yfyj LIKE '%permit%';
-- ------------------------------------------------------------
ALTER TABLE `loupan_yfyj`
  ADD COLUMN `permit_no` varchar(100) DEFAULT NULL COMMENT '预售许可证编号' AFTER `huxing_id`,
  ADD COLUMN `fwcode` varchar(100) DEFAULT NULL COMMENT '房屋编码' AFTER `permit_no`;

-- ------------------------------------------------------------
-- 2. loupan_id 允许为 NULL（原 NOT NULL）
-- ------------------------------------------------------------
ALTER TABLE `loupan_yfyj`
  MODIFY COLUMN `loupan_id` bigint DEFAULT NULL COMMENT '楼盘ID';

-- ------------------------------------------------------------
-- 3. 删除 sort 字段
--    可先执行确认：SHOW COLUMNS FROM loupan_yfyj LIKE '%sort%';
-- ------------------------------------------------------------
ALTER TABLE `loupan_yfyj` DROP COLUMN `sort`;

-- ------------------------------------------------------------
-- 4. 删除 idx_building_no 普通索引
--    可先执行确认：SHOW INDEX FROM loupan_yfyj WHERE Key_name='idx_building_no';
-- ------------------------------------------------------------
ALTER TABLE `loupan_yfyj` DROP INDEX `idx_building_no`;

-- ------------------------------------------------------------
-- 5. 唯一索引从 uk_loupan_room(loupan_id,building_no,room_no) 改为 uk_fwcode(fwcode)
--    注意：创建 uk_fwcode 前必须先清理 fwcode 重复数据（见第6步）
-- ------------------------------------------------------------
ALTER TABLE `loupan_yfyj` DROP INDEX `uk_loupan_room`;

-- ------------------------------------------------------------
-- 6. 清理 fwcode 重复数据（保留每组最小 id 一条）+ 空值置 NULL
--    仅当线上已导入大量数据时才需要执行；
--    若线上表为空或数据量小，可跳过本步直接建索引。
-- ------------------------------------------------------------
CREATE TEMPORARY TABLE tmp_keep AS
  SELECT MIN(id) AS id FROM `loupan_yfyj`
  WHERE fwcode IS NOT NULL AND fwcode <> '' GROUP BY fwcode;
DELETE FROM `loupan_yfyj`
  WHERE (fwcode IS NOT NULL AND fwcode <> '') AND id NOT IN (SELECT id FROM tmp_keep);
UPDATE `loupan_yfyj` SET fwcode = NULL WHERE fwcode = '';

-- 创建新的唯一索引
ALTER TABLE `loupan_yfyj`
  ADD UNIQUE KEY `uk_fwcode` (`fwcode`);

-- ------------------------------------------------------------
-- 7. 其余业务字段改为 DEFAULT NULL（允许为空）
-- ------------------------------------------------------------
ALTER TABLE `loupan_yfyj`
  MODIFY COLUMN `building_no` varchar(20) DEFAULT NULL COMMENT '楼栋号 如7',
  MODIFY COLUMN `room_no` varchar(20) DEFAULT NULL COMMENT '房号 301',
  MODIFY COLUMN `area` decimal(6,2) DEFAULT NULL COMMENT '建筑面积㎡',
  MODIFY COLUMN `record_unit_price` int DEFAULT NULL COMMENT '备案单价元/㎡',
  MODIFY COLUMN `record_total_price` int DEFAULT NULL COMMENT '备案总价元',
  MODIFY COLUMN `house_status` tinyint DEFAULT NULL COMMENT '房源状态：0未售 1认购 2已售 3抵押 4保留',
  MODIFY COLUMN `remark` varchar(500) DEFAULT NULL COMMENT '房源备注';

-- ============================================================
-- 验证（可选）：
--   SHOW COLUMNS FROM loupan_yfyj;
--   SHOW INDEX FROM loupan_yfyj;
-- ============================================================
