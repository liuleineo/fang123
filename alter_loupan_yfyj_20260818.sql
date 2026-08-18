-- 线上数据库 ALTER 语句：loupan_yfyj 表增加 预售证号/房屋编码 字段
-- 数据库：fang123（线上 82.156.157.118）
-- 执行前可先查询确认：SHOW COLUMNS FROM loupan_yfyj LIKE '%permit%';

ALTER TABLE `loupan_yfyj`
  ADD COLUMN `permit_no` varchar(100) DEFAULT NULL COMMENT '预售许可证编号' AFTER `huxing_id`,
  ADD COLUMN `fwcode` varchar(100) DEFAULT NULL COMMENT '房屋编码' AFTER `permit_no`;
