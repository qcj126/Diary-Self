-- =============================================
-- MQ 发件箱表（可靠消息最终一致性）
-- =============================================
CREATE TABLE IF NOT EXISTS `mq_outbox` (
                                           `id`                BIGINT          NOT NULL AUTO_INCREMENT      COMMENT '主键 ID',
                                           `event_id`          VARCHAR(64)     NOT NULL                    COMMENT '事件 ID（全局唯一）',
    `aggregate_type`    VARCHAR(64)     NOT NULL                    COMMENT '聚合根类型',
    `aggregate_id`      BIGINT          NOT NULL                    COMMENT '聚合根 ID',
    `event_type`        VARCHAR(128)    NOT NULL                    COMMENT '事件类型',
    `topic`             VARCHAR(128)    NOT NULL                    COMMENT 'MQ Topic 名称',
    `tag`               VARCHAR(64)     DEFAULT NULL                COMMENT 'MQ Tag（RocketMQ 特有）',
    `message_key`       VARCHAR(128)    DEFAULT NULL                COMMENT '消息 Key',
    `payload`           JSON            NOT NULL                    COMMENT '消息体（JSON 格式）',
    `schema_version`    INT             DEFAULT 1                   COMMENT '消息体 Schema 版本',
    `status`            VARCHAR(32)     NOT NULL                    COMMENT '消息状态：NEW/SENDING/RETRY_WAIT/SENT/DEAD',
    `retry_count`       INT             DEFAULT 0                   COMMENT '当前重试次数',
    `max_retries`       INT             DEFAULT 3                   COMMENT '最大重试次数',
    `next_retry_time`   DATETIME        DEFAULT NULL                COMMENT '下次重试时间',
    `broker_message_id` VARCHAR(128)    DEFAULT NULL                COMMENT 'MQ 返回的消息 ID',
    `last_error`        TEXT            DEFAULT NULL                COMMENT '最后一次错误信息',
    `sent_time`         DATETIME        DEFAULT NULL                COMMENT '消息发送时间',
    `create_time`       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME        NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version_id`        INT             NOT NULL    DEFAULT 0       COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status_next_retry` (`status`, `next_retry_time`),
    KEY `idx_aggregate` (`aggregate_type`, `aggregate_id`),
    KEY `idx_create_time` (`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQ 发件箱表';
