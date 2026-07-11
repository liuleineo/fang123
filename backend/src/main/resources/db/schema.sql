-- =====================================================
-- Fang123 初始建表语句
-- =====================================================
-- CREATE DATABASE IF NOT EXISTS fang123 DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =====================================================
-- 管理后台系统表 (admin 前缀，用于后台管理系统认证/授权)
-- =====================================================

-- 后台管理员表
CREATE TABLE IF NOT EXISTS `admin_user` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`        VARCHAR(64)   NOT NULL                COMMENT '用户名',
    `password`        VARCHAR(128)  NOT NULL                COMMENT '密码(BCrypt加密)',
    `nickname`        VARCHAR(64)   DEFAULT NULL            COMMENT '昵称',
    `email`           VARCHAR(128)  DEFAULT NULL            COMMENT '邮箱',
    `phone`           VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
    `avatar`          VARCHAR(256)  DEFAULT NULL            COMMENT '头像URL',
    `gender`          TINYINT       DEFAULT 0               COMMENT '性别: 0-未知, 1-男, 2-女',
    `status`          TINYINT       DEFAULT 1               COMMENT '状态: 0-禁用, 1-启用',
    `last_login_time` DATETIME      DEFAULT NULL            COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(64)   DEFAULT NULL            COMMENT '最后登录IP',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员表';

-- 后台角色表
CREATE TABLE IF NOT EXISTS `admin_role` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name`     VARCHAR(64)   NOT NULL                COMMENT '角色名称',
    `role_code`     VARCHAR(64)   NOT NULL                COMMENT '角色编码',
    `description`   VARCHAR(256)  DEFAULT NULL            COMMENT '角色描述',
    `status`        TINYINT       DEFAULT 1               COMMENT '状态: 0-禁用, 1-启用',
    `sort_order`    INT           DEFAULT 0               COMMENT '排序',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台角色表';

-- 后台管理员角色关联表
CREATE TABLE IF NOT EXISTS `admin_user_role` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT        NOT NULL                COMMENT '管理员ID(关联admin_user)',
    `role_id`       BIGINT        NOT NULL                COMMENT '角色ID(关联admin_role)',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='后台管理员角色关联表';

-- 插入默认管理员 (密码: admin123, BCrypt加密)
INSERT INTO `admin_user` (`username`, `password`, `nickname`, `status`)
VALUES ('admin', '$2b$10$k.YWnGF12dms9H5zEz.SSub8TxaKTX2Ss.zJFyiNZ9x/7VzZUgOUe', '超级管理员', 1)
ON DUPLICATE KEY UPDATE `username`=`username`;

-- 插入默认角色
INSERT INTO `admin_role` (`role_name`, `role_code`, `description`)
VALUES ('超级管理员', 'ROLE_ADMIN', '系统超级管理员')
ON DUPLICATE KEY UPDATE `role_code`=`role_code`;

-- =====================================================
-- 业务用户模块表 (user_ 前缀，面向 C 端用户)
-- =====================================================

-- 用户主表
CREATE TABLE IF NOT EXISTS `user_info` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `nickname`    VARCHAR(50)  DEFAULT ''                COMMENT '用户昵称',
    `avatar`      VARCHAR(255) DEFAULT ''                COMMENT '用户头像',
    `phone`       VARCHAR(20)  DEFAULT ''                COMMENT '手机号',
    `sex`         TINYINT      DEFAULT 0                 COMMENT '性别：0未知 1男 2女',
    `status`      TINYINT      NOT NULL DEFAULT 1        COMMENT '状态：0禁用 1正常',
    `invite_code` VARCHAR(32)  DEFAULT ''                COMMENT '个人邀请码',
    `parent_id`   BIGINT       DEFAULT 0                 COMMENT '上级邀请用户ID',
    `deleted`     TINYINT      NOT NULL DEFAULT 0        COMMENT '逻辑删除',
    `last_login_time` DATETIME  DEFAULT NULL            COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(64) DEFAULT NULL          COMMENT '最后登录IP',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    KEY `idx_phone` (`phone`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户主表';

-- 用户微信绑定表
CREATE TABLE IF NOT EXISTS `user_wechat` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
    `openid`      VARCHAR(100) NOT NULL DEFAULT ''     COMMENT '微信openid',
    `unionid`     VARCHAR(100) DEFAULT ''              COMMENT '微信unionid',
    `app_type`    VARCHAR(20)  DEFAULT ''              COMMENT '应用类型',
    `nickname`    VARCHAR(100) DEFAULT ''              COMMENT '微信昵称',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户微信绑定表';

-- 用户收货地址表
CREATE TABLE IF NOT EXISTS `user_address` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
    `name`        VARCHAR(50)  NOT NULL DEFAULT ''     COMMENT '收货人姓名',
    `phone`       VARCHAR(20)  NOT NULL DEFAULT ''     COMMENT '收货人电话',
    `province`    VARCHAR(50)  DEFAULT ''              COMMENT '省份',
    `city`        VARCHAR(50)  DEFAULT ''              COMMENT '城市',
    `district`    VARCHAR(50)  DEFAULT ''              COMMENT '区县',
    `detail`      VARCHAR(255) DEFAULT ''              COMMENT '详细地址',
    `is_default`  TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认：0否 1是',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址表';

-- =====================================================
-- 积分模块表
-- =====================================================

CREATE TABLE IF NOT EXISTS `point_user` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `total_point` INT      NOT NULL DEFAULT 0      COMMENT '累计积分',
    `usable_point` INT     NOT NULL DEFAULT 0      COMMENT '可用积分',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户积分表';

CREATE TABLE IF NOT EXISTS `point_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户ID',
    `point`       INT          NOT NULL DEFAULT 0      COMMENT '变动积分',
    `balance`     INT          NOT NULL DEFAULT 0      COMMENT '变动后可用积分',
    `type`        TINYINT      NOT NULL DEFAULT 1      COMMENT '类型：1获取 2消耗',
    `source`      VARCHAR(50)  DEFAULT ''              COMMENT '来源',
    `remark`      VARCHAR(255) DEFAULT ''              COMMENT '变动备注',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_create` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分流水记录表';

-- =====================================================
-- 会员模块表
-- =====================================================

CREATE TABLE IF NOT EXISTS `member_package` (
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
    `package_name` VARCHAR(100)   NOT NULL DEFAULT ''     COMMENT '套餐名称',
    `price`        DECIMAL(10,2)  NOT NULL DEFAULT 0.00   COMMENT '套餐价格',
    `day_num`      INT            NOT NULL DEFAULT 0      COMMENT '有效天数',
    `give_point`   INT            NOT NULL DEFAULT 0      COMMENT '赠送积分',
    `sort`         INT            NOT NULL DEFAULT 0      COMMENT '排序',
    `status`       TINYINT        NOT NULL DEFAULT 1      COMMENT '状态：0下架 1上架',
    `deleted`      TINYINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    `create_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员套餐表';

CREATE TABLE IF NOT EXISTS `member_user` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `is_member`   TINYINT  NOT NULL DEFAULT 0      COMMENT '是否会员：0否 1是',
    `expire_time` DATETIME DEFAULT NULL            COMMENT '会员过期时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开通时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户会员表';

-- =====================================================
-- 邀请模块表
-- =====================================================

CREATE TABLE IF NOT EXISTS `invite_user` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
    `parent_id`   BIGINT   NOT NULL DEFAULT 0      COMMENT '上级用户ID',
    `invite_num`  INT      NOT NULL DEFAULT 0      COMMENT '累计邀请人数',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    INDEX `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户邀请关系表';

CREATE TABLE IF NOT EXISTS `invite_reward_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT       NOT NULL                COMMENT '获奖用户ID',
    `invite_user_id`  BIGINT       NOT NULL                COMMENT '被邀请用户ID',
    `reward_type`     TINYINT      NOT NULL DEFAULT 1      COMMENT '奖励类型：1积分',
    `reward_num`      INT          NOT NULL DEFAULT 0      COMMENT '奖励数量',
    `remark`          VARCHAR(255) DEFAULT ''              COMMENT '奖励备注',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邀请奖励记录表';

-- =====================================================
-- 系统配置表
-- =====================================================

CREATE TABLE IF NOT EXISTS `sys_config` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key`  VARCHAR(50)  NOT NULL DEFAULT ''       COMMENT '配置键名',
    `config_name` VARCHAR(100) NOT NULL DEFAULT ''       COMMENT '配置名称',
    `config_value` VARCHAR(500) DEFAULT ''               COMMENT '配置值',
    `remark`      VARCHAR(255) DEFAULT ''               COMMENT '备注说明',
    `status`      TINYINT      NOT NULL DEFAULT 1        COMMENT '状态：0禁用 1启用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0        COMMENT '逻辑删除：0正常 1删除',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统全局配置表';

-- =====================================================
-- 任务模块表
-- =====================================================

CREATE TABLE IF NOT EXISTS `task_info` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '任务主键ID',
    `task_title`     VARCHAR(200) NOT NULL DEFAULT ''   COMMENT '任务标题',
    `task_cover`     VARCHAR(255) DEFAULT ''            COMMENT '任务封面图',
    `task_intro`     VARCHAR(500) DEFAULT ''            COMMENT '任务简介',
    `keyword`        VARCHAR(200) DEFAULT ''            COMMENT '搜索关键词',
    `task_content`   TEXT                                COMMENT '任务操作步骤详情',
    `reward_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '奖励金额',
    `gift`           VARCHAR(255) DEFAULT ''            COMMENT '赠品',
    `total_num`      INT          NOT NULL DEFAULT 0    COMMENT '总数量(24小时之和)',
    `hour_limits`    VARCHAR(200) DEFAULT ''            COMMENT '24小时每小时数量,逗号分隔',
    `expire_minute`  INT          NOT NULL DEFAULT 60   COMMENT '有效时长(分钟)',
    `sort`           INT          NOT NULL DEFAULT 0    COMMENT '排序',
    `status`         TINYINT      NOT NULL DEFAULT 0    COMMENT '0下架1上架',
    `deleted`        TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status_sort` (`status`, `sort`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务配置主表';

CREATE TABLE IF NOT EXISTS `task_user_order` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id`         BIGINT       NOT NULL                COMMENT '用户ID',
    `task_id`         BIGINT       NOT NULL                COMMENT '任务ID',
    `openid`          VARCHAR(100) DEFAULT ''              COMMENT '微信openid',
    `reward_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '奖励金额',
    `gift`            VARCHAR(255) DEFAULT ''              COMMENT '赠品',
    `audit_status`    TINYINT      NOT NULL DEFAULT 1      COMMENT '1待提交2待审核3通过4驳回5过期',
    `grant_pay`       TINYINT      NOT NULL DEFAULT 0      COMMENT '授权打款0未授权1已授权',
    `withdraw_status` TINYINT      NOT NULL DEFAULT 0      COMMENT '0未提现1处理中2成功3失败',
    `expire_time`     DATETIME     NULL                    COMMENT '过期时间',
    `submit_images`   VARCHAR(1000) DEFAULT ''             COMMENT '截图',
    `submit_note`     VARCHAR(500) DEFAULT ''              COMMENT '备注/订单号',
    `audit_note`      VARCHAR(500) DEFAULT ''              COMMENT '审核备注',
    `audit_time`      DATETIME     NULL                    COMMENT '审核时间',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_task` (`user_id`, `task_id`),
    INDEX `idx_audit_status` (`audit_status`),
    INDEX `idx_user_create` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户任务订单表';

CREATE TABLE IF NOT EXISTS `task_pay_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `order_id`      BIGINT       NOT NULL                COMMENT '订单ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '用户ID',
    `task_title`    VARCHAR(200) DEFAULT ''              COMMENT '任务名称',
    `trade_no`      VARCHAR(64)  NOT NULL DEFAULT ''     COMMENT '交易单号',
    `wechat_pay_no` VARCHAR(64)  DEFAULT ''              COMMENT '微信单号',
    `pay_amount`    DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '打款金额',
    `pay_status`    TINYINT      NOT NULL DEFAULT 1      COMMENT '1处理中2成功3失败',
    `fail_reason`   VARCHAR(500) DEFAULT ''              COMMENT '失败原因',
    `retry_count`   INT          NOT NULL DEFAULT 0      COMMENT '重试次数',
    `apply_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `pay_time`      DATETIME     NULL                    COMMENT '打款时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_no` (`trade_no`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_pay_status` (`pay_status`),
    INDEX `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务打款日志表';

-- =====================================================
-- 房产模块表
-- =====================================================

-- 1. 土拍地块表
CREATE TABLE IF NOT EXISTS `loupan_tupai_land` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '土拍地块主键ID',
  `loupan_id` bigint DEFAULT NULL COMMENT '关联楼盘ID，开发后绑定对应楼盘',
  `land_name` varchar(200) NOT NULL COMMENT '地块名称',
  `land_no` varchar(100) NOT NULL COMMENT '宗地编号，唯一',
  `district` varchar(50) NOT NULL COMMENT '所属行政区',
  `plate` varchar(50) DEFAULT '' COMMENT '所属板块',
  `land_scope` text COMMENT '地块四至范围',
  `land_status` tinyint NOT NULL DEFAULT 0 COMMENT '地块状态：0待出让 1已出让 2流拍',
  `winner_company` varchar(100) NOT NULL COMMENT '竞得方企业',
  `land_use_type` varchar(100) NOT NULL COMMENT '土地用途',
  `land_year` int NOT NULL DEFAULT 70 COMMENT '土地使用年限',
  `land_area` decimal(10,2) NOT NULL COMMENT '占地面积 ㎡',
  `build_area_limit` decimal(12,2) DEFAULT NULL COMMENT '最大可建建筑面积 ㎡',
  `plot_ratio` decimal(3,2) DEFAULT NULL COMMENT '容积率上限',
  `start_price` bigint NOT NULL DEFAULT 0 COMMENT '竞拍起价 万元',
  `deal_price` bigint NOT NULL DEFAULT 0 COMMENT '成交总价 万元',
  `floor_unit_price` int DEFAULT NULL COMMENT '楼面单价 元/㎡',
  `premium_rate` decimal(5,2) DEFAULT NULL COMMENT '溢价率 %',
  `deal_date` date NOT NULL COMMENT '成交日期',
  `longitude` decimal(12,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(12,6) DEFAULT NULL COMMENT '纬度',
  `resource_id` varchar(100) DEFAULT NULL COMMENT '资源ID',
  `location_image` varchar(500) DEFAULT NULL COMMENT '位置示意图URL',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序权重',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0正常 1已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_land_no` (`land_no`),
  KEY `idx_loupan_id` (`loupan_id`),
  KEY `idx_district_plate` (`district`, `plate`),
  KEY `idx_deal_date` (`deal_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='土拍地块信息表';

-- 测试数据：潮映杭园对应地块
INSERT INTO `loupan_tupai_land` (
`loupan_id`,`land_name`,`land_no`,`district`,`plate`,`land_scope`,`land_status`,
`winner_company`,`land_use_type`,`land_year`,`land_area`,`build_area_limit`,
`plot_ratio`,`start_price`,`deal_price`,`floor_unit_price`,`premium_rate`,`deal_date`,`sort`
) VALUES (
1,'开发区单元JS0903-20地块','杭政储出[2025]45号','钱塘区','金沙湖',
'东至空地，南至12号大街规划绿化，西至规划景园路，北至规划支路',
1,'建杭','普通商品住房用地（二类）',70,63599.00,89038.60,1.40,
133567,133567,15001,0.00,'2025-04-28',0
);

-- 2. 楼盘主表
CREATE TABLE IF NOT EXISTS `loupan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '楼盘主键ID',
  `land_id` bigint DEFAULT NULL COMMENT '对应土拍地块ID，关联loupan_tupai_land.id',
  `land_no` varchar(100) NOT NULL COMMENT '宗地编号，唯一',
  `cover_image` varchar(500) DEFAULT '' COMMENT '封面图URL',
  `project_name` varchar(100) NOT NULL COMMENT '楼盘名称',
  `short_name` varchar(50) DEFAULT '' COMMENT '楼盘简称',
  `district` varchar(50) NOT NULL DEFAULT '' COMMENT '行政区，如钱塘区',
  `plate` varchar(50) NOT NULL DEFAULT '' COMMENT '板块，如金沙湖',
  `avg_unit_price` int DEFAULT NULL COMMENT '楼盘均价 元/㎡',
  `min_total_price` int DEFAULT NULL COMMENT '最低总价 万元',
  `max_total_price` int DEFAULT NULL COMMENT '最高总价 万元',
  `price_tag` varchar(100) DEFAULT '' COMMENT '价格标签，逗号分隔：刚需/改善/限价',
  `sales_address` varchar(200) NOT NULL COMMENT '售楼部位置',
  `sales_status` tinyint NOT NULL DEFAULT 0 COMMENT '售楼状态：0待开放 1在售 2售罄 3停工 4交付',
  `sales_tel` varchar(50) DEFAULT '' COMMENT '售楼电话',
  `project_address` varchar(200) NOT NULL COMMENT '楼盘地址',
  `show_house_desc` text COMMENT '样板房位置说明',
  `delivery_date` date DEFAULT NULL COMMENT '交房时间',
  `floor_height_min` decimal(3,1) DEFAULT NULL COMMENT '最低层高(m)',
  `floor_height_max` decimal(3,1) DEFAULT NULL COMMENT '最高层高(m)',
  `building_total` int NOT NULL DEFAULT 0 COMMENT '楼栋总数',
  `floor_min` int DEFAULT 0 COMMENT '最低楼层',
  `floor_max` int DEFAULT 0 COMMENT '最高楼层',
  `area_min` int NOT NULL DEFAULT 0 COMMENT '最小户型面积(㎡)',
  `area_max` int NOT NULL DEFAULT 0 COMMENT '最大户型面积(㎡)',
  `decorate_type` tinyint NOT NULL DEFAULT 1 COMMENT '装修类型：1精装 2毛坯 3简装',
  `property_right_year` int NOT NULL DEFAULT 70 COMMENT '产权年限',
  `house_type` tinyint NOT NULL DEFAULT 1 COMMENT '楼盘类型：1住宅 2公寓 3商铺 4别墅',
  `community_facility` text COMMENT '小区配套设施，逗号分隔',
  `people_car_separate` tinyint NOT NULL DEFAULT 1 COMMENT '是否人车分流：0否 1是',
  `property_fee_high` decimal(4,2) DEFAULT NULL COMMENT '小高/洋房物业费 元/㎡/月',
  `property_fee_villa` decimal(4,2) DEFAULT NULL COMMENT '排屋别墅物业费 元/㎡/月',
  `property_company` varchar(100) NOT NULL COMMENT '物业公司名称',
  `park_total` int NOT NULL DEFAULT 0 COMMENT '总车位数量',
  `park_sell_num` int NOT NULL DEFAULT 0 COMMENT '可售车位数量',
  `park_ratio` varchar(20) DEFAULT '' COMMENT '车位配比',
  `facade_material` text COMMENT '外立面材料说明',
  `self_hold_rate` decimal(5,2) DEFAULT 0.00 COMMENT '自持率%',
  `build_area` bigint NOT NULL DEFAULT 0 COMMENT '总建筑面积(㎡)',
  `land_area` bigint NOT NULL DEFAULT 0 COMMENT '占地面积(㎡)',
  `house_total` int NOT NULL DEFAULT 0 COMMENT '总户数',
  `plot_ratio` decimal(3,2) NOT NULL DEFAULT 0.00 COMMENT '容积率',
  `green_rate` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT '绿地率%',
  `project_company` varchar(100) NOT NULL COMMENT '项目开发公司',
  `brand_list` varchar(200) NOT NULL COMMENT '开发品牌，逗号分隔',
  `land_price` bigint NOT NULL DEFAULT 0 COMMENT '拿地总价（万元）',
  `land_unit_price` int NOT NULL DEFAULT 0 COMMENT '楼面单价 元/㎡',
  `land_buy_date` date DEFAULT NULL COMMENT '拿地日期',
  `edu_support` text COMMENT '教育配套，逗号分隔',
  `traffic_support` text COMMENT '交通配套，逗号分隔',
  `medical_support` text COMMENT '医疗配套，逗号分隔',
  `business_support` text COMMENT '商业配套，逗号分隔',
  `view_support` text COMMENT '景观公园配套，逗号分隔',
  `longitude` decimal(12,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(12,6) DEFAULT NULL COMMENT '纬度',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序权重',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0正常 1已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_project_name` (`project_name`),
  KEY `idx_district_plate` (`district`, `plate`),
  KEY `idx_land_id` (`land_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼盘主信息表';

-- 潮映杭园测试数据
INSERT INTO `loupan` (
`land_id`,`project_name`,`district`,`plate`,`avg_unit_price`,`min_total_price`,`max_total_price`,`price_tag`,
`sales_address`,`sales_status`,`sales_tel`,`project_address`,`show_house_desc`,
`delivery_date`,`floor_height_min`,`floor_height_max`,`building_total`,`floor_min`,`floor_max`,
`area_min`,`area_max`,`decorate_type`,`property_right_year`,`house_type`,
`community_facility`,`people_car_separate`,`property_fee_high`,`property_fee_villa`,
`property_company`,`park_total`,`park_sell_num`,`park_ratio`,`facade_material`,`self_hold_rate`,
`build_area`,`land_area`,`house_total`,`plot_ratio`,`green_rate`,`project_company`,`brand_list`,
`land_price`,`land_unit_price`,`land_buy_date`,
`edu_support`,`traffic_support`,`medical_support`,`business_support`,`view_support`,`sort`
) VALUES (
1,'潮映杭园','钱塘区','金沙湖',NULL,NULL,NULL,'改善,精装住宅',
'三号大街和十二号大街交叉口',0,'0571-88888288','杭州市钱塘区白杨街道12号大街和3号大街交汇处',
'7号楼3层展示样板间105方，127方，130方，7号楼二层交付样板间105方，127方，130方',
'2027-12-31',3.0,4.2,29,4,17,105,330,1,70,1,
'网球场,中芯水系,架空主题',1,3.50,5.60,'滨江物业',782,760,'1:1.2',
'小高层三面铝板，洋房和排屋四面铝板',0.00,147554,63599,649,1.40,35.00,
'杭州建潮房地产开发有限公司','建杭,滨江,钱塘建投',133567,15001,'2025-04-28',
'金沙湖实验学校、文海第二实验学校、杭州实验外国语学校、文海钱塘汇学校(规划)',
'高沙路地铁1号线约1.5公里、钱塘快速路、德胜快速路、东湖高架',
'浙江大学医学院附属邵逸夫医院(钱塘院区)、浙江省中医院(钱塘院区)',
'银泰百货、金沙印象城、龙湖杭州金沙天街、龙湖杭州吾角天街',
'金沙湖公园，下沙沿江湿地公园、自带超2万方中央景观公园',0
);

-- 3. 楼盘户型表
CREATE TABLE IF NOT EXISTS `loupan_huxing` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '户型ID',
  `loupan_id` bigint NOT NULL COMMENT '关联楼盘ID',
  `huxing_name` varchar(50) NOT NULL COMMENT '户型名称 如105㎡三房两厅',
  `area` int NOT NULL COMMENT '建筑面积㎡',
  `inside_area` int DEFAULT 0 COMMENT '套内面积㎡',
  `room_num` tinyint NOT NULL DEFAULT 0 COMMENT '卧室数量',
  `hall_num` tinyint NOT NULL DEFAULT 0 COMMENT '厅数量(客餐)',
  `toilet_num` tinyint NOT NULL DEFAULT 0 COMMENT '卫生间数量',
  `balcony_num` tinyint DEFAULT 0 COMMENT '阳台数量',
  `orientation` varchar(20) DEFAULT '' COMMENT '朝向 南北/东南',
  `floor_type` tinyint NOT NULL DEFAULT 1 COMMENT '产品类型：1小高层 2洋房 3叠墅 4排屋',
  `unit_price` int DEFAULT NULL COMMENT '单价元/㎡',
  `total_price_start` int DEFAULT NULL COMMENT '总价起步万元',
  `total_price_end` int DEFAULT NULL COMMENT '总价封顶万元',
  `is_show_house` tinyint NOT NULL DEFAULT 0 COMMENT '是否有样板间：0无 1有',
  `tag` varchar(200) DEFAULT '' COMMENT '户型标签，逗号分隔',
  `huxing_image` varchar(500) DEFAULT NULL COMMENT '户型图URL',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0正常 1已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_loupan_id` (`loupan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼盘户型表';

-- 样板户型测试数据
INSERT INTO `loupan_huxing` (`loupan_id`,`huxing_name`,`area`,`room_num`,`hall_num`,`toilet_num`,`floor_type`,`is_show_house`,`sort`)
VALUES
(1,'105㎡三房两厅两卫',105,3,2,2,1,1,0),
(1,'127㎡四房两厅两卫',127,4,2,2,1,1,1),
(1,'130㎡四房两厅三卫',130,4,2,3,2,1,2),
(1,'330㎡排屋',330,5,3,4,3,0,3);

-- 4. 视频图库
CREATE TABLE IF NOT EXISTS `loupan_media` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '素材ID',
  `loupan_id` bigint NOT NULL COMMENT '关联楼盘ID',
  `huxing_id` bigint DEFAULT NULL COMMENT '关联户型ID，户型图绑定',
  `media_type` tinyint NOT NULL COMMENT '素材类型：1实景图 2样板间 3户型图 4航拍 5短视频 6VR 7设计图 8区位图 9效果图 10施工进度 11周边配套',
  `media_url` varchar(500) NOT NULL COMMENT '素材CDN/OSS地址',
  `media_title` varchar(100) DEFAULT '' COMMENT '素材标题',
  `sort` int NOT NULL DEFAULT 0 COMMENT '展示排序',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0正常 1已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_loupan_id` (`loupan_id`),
  KEY `idx_huxing_id` (`huxing_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼盘视频图库';

-- 5. 一房一价表
CREATE TABLE IF NOT EXISTS `loupan_yfyj` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房源ID',
  `loupan_id` bigint NOT NULL COMMENT '楼盘ID',
  `huxing_id` bigint DEFAULT NULL COMMENT '对应户型ID，可选',
  `building_no` varchar(20) NOT NULL COMMENT '楼栋号 7号楼',
  `floor_no` int NOT NULL COMMENT '楼层',
  `room_no` varchar(20) NOT NULL COMMENT '房号 7-301',
  `area` decimal(6,2) NOT NULL COMMENT '建筑面积㎡',
  `record_unit_price` int NOT NULL COMMENT '备案单价元/㎡',
  `record_total_price` int NOT NULL COMMENT '备案总价元',
  `sale_unit_price` int DEFAULT NULL COMMENT '销售单价元/㎡',
  `sale_total_price` int DEFAULT NULL COMMENT '销售总价元',
  `house_status` tinyint NOT NULL DEFAULT 0 COMMENT '房源状态：0未售 1认购 2已售 3抵押 4保留',
  `orientation` varchar(20) DEFAULT '' COMMENT '单套朝向',
  `remark` varchar(500) DEFAULT '' COMMENT '房源备注',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0正常 1已删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_loupan_id` (`loupan_id`),
  KEY `idx_building_floor` (`building_no`,`floor_no`),
  UNIQUE KEY `uk_loupan_room` (`loupan_id`,`building_no`,`room_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一房一价房源表';

--预售证表
CREATE TABLE `loupan_presale_permit` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `loupan_id` bigint DEFAULT NULL COMMENT '楼盘ID',
  `project_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `publicity_end_date` DATE DEFAULT NULL COMMENT '公示结束日期',
  `develop_company` VARCHAR(150) NOT NULL COMMENT '开发公司',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '坐落位置',
  `sale_address` VARCHAR(255) DEFAULT NULL COMMENT '销售部地址',
  `sale_phone` VARCHAR(50) DEFAULT NULL COMMENT '销售部电话',
  `online_sale_area` DECIMAL(12,2) DEFAULT NULL COMMENT '纳入网上预(销)售总面积(㎡)',
  `permit_no` VARCHAR(100) NOT NULL COMMENT '预售许可证编号',
  `permit_no_str` VARCHAR(100) NOT NULL COMMENT '预售许可证编号STR',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除：0正常 1已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permit_no_str` (`permit_no_str`) COMMENT '预售证编号STR唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼盘预售许可证信息表';