-- diary-file 图片上传完成事件的发件箱。
-- 在 image 表所在的同一个数据库中执行，保证图片状态变更与事件写入可使用同一个本地事务。
-- 这是新增表，不修改 diary-AI 的 mq_outbox，也不迁移历史 RabbitMQ 消息。
CREATE TABLE file_mq_outbox (
    id                BIGINT NOT NULL COMMENT '发件箱记录主键',
    event_id          VARCHAR(64) NOT NULL COMMENT '业务事件唯一 ID；重发时保持不变',
    image_id          BIGINT NOT NULL COMMENT '关联的 image.id',
    event_type        VARCHAR(64) NOT NULL COMMENT '事件类型，当前为 IMAGE_UPLOADED',
    topic             VARCHAR(128) NOT NULL COMMENT '事件投递的 RocketMQ Topic',
    tag               VARCHAR(64) NOT NULL COMMENT '事件投递的 RocketMQ Tag',
    message_key       VARCHAR(128) NOT NULL COMMENT 'RocketMQ 消息检索 Key，当前使用 event_id',
    payload           JSON NOT NULL COMMENT '固定的事件 JSON；重发时不重新构造',
    schema_version    INT NOT NULL COMMENT '消息协议版本，当前 IMAGE_UPLOADED 为 1',
    status            VARCHAR(32) NOT NULL COMMENT '发布状态：NEW、SENDING、RETRY_WAIT、SENT、DEAD',
    retry_count       INT NOT NULL DEFAULT 0 COMMENT '额外发送重试次数，不含首次发送',
    max_retries       INT NOT NULL COMMENT '允许的最大额外发送重试次数',
    next_retry_time   DATETIME NOT NULL COMMENT '下一次可领取发送的时间',
    broker_message_id VARCHAR(128) NULL COMMENT 'RocketMQ 确认发送后返回的消息 ID',
    last_error        VARCHAR(500) NULL COMMENT '最近一次发送失败或超时的原因',
    sent_time         DATETIME NULL COMMENT '确认消息已发送到 Broker 的时间',
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发件箱记录创建时间',
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最近更新时间；用于判断 SENDING 超时',
    version_id        INT NOT NULL DEFAULT 0 COMMENT '领取与确认发送时使用的乐观锁版本',

    PRIMARY KEY (id),
    UNIQUE KEY uk_file_outbox_event_id (event_id),
    UNIQUE KEY uk_file_outbox_image_event (image_id, event_type),
    KEY idx_file_outbox_ready (status, next_retry_time, id),
    KEY idx_file_outbox_sending (status, update_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='diary-file 图片事件发件箱';
