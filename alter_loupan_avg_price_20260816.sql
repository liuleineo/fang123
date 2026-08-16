-- 线上数据库 ALTER 语句：loupan 表增加 洋房/叠墅/排屋 均价字段
-- 数据库：fang123（线上 82.156.157.118）
-- 执行前可先查询确认：SHOW COLUMNS FROM loupan LIKE 'avg_unit_price%';

ALTER TABLE `loupan`
  ADD COLUMN `avg_unit_price_yangfang` int DEFAULT NULL COMMENT '洋房均价 元/㎡' AFTER `avg_unit_price`,
  ADD COLUMN `avg_unit_price_dieshu` int DEFAULT NULL COMMENT '叠墅均价 元/㎡' AFTER `avg_unit_price_yangfang`,
  ADD COLUMN `avg_unit_price_paiwu` int DEFAULT NULL COMMENT '排屋均价 元/㎡' AFTER `avg_unit_price_dieshu`;
