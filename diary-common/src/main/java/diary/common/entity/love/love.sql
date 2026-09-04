-- 恋爱纪念册数据表（MySQL 8.0+）
--
-- 设计说明：
-- 1. 所有 BIGINT 主键均由应用侧生成（例如 Snowflake），不使用 AUTO_INCREMENT。
-- 2. 图片复用公共 image 表，通过 love_record_image.image_id 关联 image.id。
-- 3. 为适配微服务架构，本脚本不创建物理外键，数据一致性由业务层维护。
-- 4. 时间线的按周/月统计、年度回顾、地点访问次数均从 love_record 聚合计算。
-- 5. 经度、纬度采用 WGS84 坐标系；MySQL 8 可基于普通索引完成当前数据量查询。

CREATE TABLE IF NOT EXISTS `love_couple` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `owner_user_id`   BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
    `partner_user_id` BIGINT UNSIGNED NULL COMMENT '对方用户ID；对方未注册时为空',
    `partner_name`    VARCHAR(32) NOT NULL COMMENT '对方昵称',
    `start_date`      DATE NOT NULL COMMENT '在一起的第一天',
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-正常',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_love_couple_owner` (`owner_user_id`, `deleted`),
    KEY `idx_love_couple_partner` (`partner_user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱关系表';

CREATE TABLE IF NOT EXISTS `love_anniversary` (
    `id`             BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `couple_id`      BIGINT UNSIGNED NOT NULL COMMENT '恋爱关系ID',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
    `name`           VARCHAR(64) NOT NULL COMMENT '纪念日名称',
    `event_date`     DATE NOT NULL COMMENT '纪念日原始日期',
    `repeat_type`    TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '重复类型：0-不重复，1-每年重复',
    `remind_days`    SMALLINT UNSIGNED NOT NULL DEFAULT 7 COMMENT '提前提醒天数',
    `pinned`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
    `sort`           INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `deleted`        TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_love_anniversary_upcoming` (`couple_id`, `deleted`, `event_date`),
    KEY `idx_love_anniversary_pinned` (`couple_id`, `pinned`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱纪念日表';

CREATE TABLE IF NOT EXISTS `love_location` (
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `couple_id`   BIGINT UNSIGNED NOT NULL COMMENT '恋爱关系ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '地点名称',
    `address`     VARCHAR(255) NULL COMMENT '详细地址',
    `longitude`   DECIMAL(10, 7) NULL COMMENT '经度，WGS84',
    `latitude`    DECIMAL(10, 7) NULL COMMENT '纬度，WGS84',
    `city_code`   VARCHAR(20) NULL COMMENT '城市行政区划编码',
    `city_name`   VARCHAR(64) NULL COMMENT '城市名称',
    `deleted`     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_love_location_couple` (`couple_id`, `deleted`),
    KEY `idx_love_location_name` (`couple_id`, `name`),
    KEY `idx_love_location_coordinate` (`longitude`, `latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱足迹地点表';

CREATE TABLE IF NOT EXISTS `love_record` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `couple_id`       BIGINT UNSIGNED NOT NULL COMMENT '恋爱关系ID',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
    `location_id`     BIGINT UNSIGNED NULL COMMENT '地点ID，对应 love_location.id',
    `title`           VARCHAR(100) NOT NULL COMMENT '记录标题',
    `content`         TEXT NULL COMMENT '完整记录内容',
    `record_date`     DATE NOT NULL COMMENT '记录发生日期',
    `category_code`   VARCHAR(32) NOT NULL COMMENT '分类编码：DATE-约会，DAILY-日常，TRAVEL-旅行，ANNIVERSARY-纪念日',
    `important`      TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否重要回忆：0-否，1-是',
    `sort`            INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '同日期内排序值，越小越靠前',
    `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_love_record_timeline` (`couple_id`, `deleted`, `record_date` DESC, `id` DESC),
    KEY `idx_love_record_category` (`couple_id`, `category_code`, `deleted`, `record_date` DESC),
    KEY `idx_love_record_location` (`location_id`, `deleted`, `record_date` DESC),
    KEY `idx_love_record_creator` (`creator_user_id`, `deleted`),
    KEY `idx_love_record_important` (`couple_id`, `important`, `deleted`, `record_date` DESC),
    CONSTRAINT `chk_love_record_category`
        CHECK (`category_code` IN ('DATE', 'DAILY', 'TRAVEL', 'ANNIVERSARY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱记录主表';

CREATE TABLE IF NOT EXISTS `love_record_image` (
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `record_id`   BIGINT UNSIGNED NOT NULL COMMENT '恋爱记录ID',
    `image_id`    BIGINT UNSIGNED NOT NULL COMMENT '公共图片ID，对应 image.id',
    `is_cover`    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否封面：0-否，1-是',
    `sort`        INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '图片排序值，越小越靠前',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_love_record_image` (`record_id`, `image_id`),
    KEY `idx_love_record_image_sort` (`record_id`, `sort`),
    KEY `idx_love_record_image_id` (`image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱记录图片关联表';

CREATE TABLE IF NOT EXISTS `love_mood` (
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `record_id`   BIGINT UNSIGNED NOT NULL COMMENT '恋爱记录ID',
    `mood_code`   VARCHAR(32) NOT NULL COMMENT '心情编码',
    `mood_name`   VARCHAR(32) NOT NULL COMMENT '心情名称',
    `emoji`       VARCHAR(16) NULL COMMENT '展示用 Emoji',
    `sort`        INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    `enabled`     TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_love_mood_code` (`mood_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱记录心情字典表';

CREATE TABLE IF NOT EXISTS `love_tag` (
    `id`              BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `record_id`       BIGINT UNSIGNED NOT NULL COMMENT '恋爱记录ID',
    `couple_id`       BIGINT UNSIGNED NOT NULL COMMENT '恋爱关系ID',
    `creator_user_id` BIGINT UNSIGNED NOT NULL COMMENT '创建者用户ID',
    `tag_name`        VARCHAR(32) NOT NULL COMMENT '标签名称，不包含#',
    `color`           VARCHAR(16) NULL COMMENT '展示颜色，例如 #FF6B81',
    `sort`            INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '展示排序值',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_love_tag_couple` (`couple_id`),
    KEY `idx_love_tag_name` (`couple_id`, `tag_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='恋爱记录自定义标签表';

CREATE TABLE IF NOT EXISTS `love_menstrual_cycle` (
    `id`                BIGINT UNSIGNED NOT NULL COMMENT '主键',
    `couple_id`         BIGINT UNSIGNED NOT NULL COMMENT '恋爱关系ID',
    `subject_user_id`   BIGINT UNSIGNED NULL COMMENT '生理期所属用户ID；未注册时为空',
    `recorder_user_id`  BIGINT UNSIGNED NOT NULL COMMENT '记录者用户ID',
    `period_start_date` DATE NOT NULL COMMENT '本次生理期开始日期',
    `period_end_date`   DATE NULL COMMENT '本次生理期结束日期',
    `cycle_length`      SMALLINT UNSIGNED NOT NULL DEFAULT 28 COMMENT '周期长度（天）',
    `period_length`     SMALLINT UNSIGNED NOT NULL DEFAULT 5 COMMENT '经期长度（天）',
    `symptoms`          JSON NULL COMMENT '症状 JSON 数组，例如 ["腹痛","疲惫"]',
    `note`              VARCHAR(500) NULL COMMENT '备注或关怀记录',
    `privacy_scope`     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '可见范围：0-仅本人，1-双方可见',
    `deleted`           TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
    `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_love_cycle_timeline` (`couple_id`, `deleted`, `period_start_date` DESC),
    KEY `idx_love_cycle_subject` (`subject_user_id`, `deleted`, `period_start_date` DESC),
    CONSTRAINT `chk_love_cycle_dates`
        CHECK (`period_end_date` IS NULL OR `period_end_date` >= `period_start_date`),
    CONSTRAINT `chk_love_cycle_length`
        CHECK (`cycle_length` BETWEEN 15 AND 90),
    CONSTRAINT `chk_love_period_length`
        CHECK (`period_length` BETWEEN 1 AND 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生理期记录表';

-- 初始化前端所需的心情字典。固定 ID 仅用于基础字典数据。
INSERT INTO `love_mood` (`id`, `mood_code`, `mood_name`, `emoji`, `sort`, `enabled`)
VALUES
    (1000, 'HEARTBEAT', '心动', '❤️', 10, 1),
    (1100, 'HAPPY',     '开心', '😊', 20, 1),
    (1200, 'HEALING',   '治愈', '🌿', 30, 1),
    (1300, 'TOUCHED',   '感动', '🥹', 40, 1),
    (1400, 'CALM',      '平静', '☁️', 50, 1),
    (1500, 'LAUGH',     '爆笑', '😆', 60, 1)
ON DUPLICATE KEY UPDATE
    `mood_name` = VALUES(`mood_name`),
    `emoji` = VALUES(`emoji`),
    `sort` = VALUES(`sort`),
    `enabled` = VALUES(`enabled`),
    `update_time` = CURRENT_TIMESTAMP;

