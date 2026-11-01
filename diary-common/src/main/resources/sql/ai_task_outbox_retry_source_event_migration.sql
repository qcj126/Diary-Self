-- AI_TASK_RETRY 消费幂等迁移。
-- 同一条来源 MQ 消息至多安排一条下一轮 Retry Outbox；NULL 保持兼容非消费触发的历史事件。
ALTER TABLE mq_outbox
    ADD COLUMN source_event_id VARCHAR(64) NULL
        COMMENT '触发当前 Retry 的来源消息 ID' AFTER event_id,
    ADD UNIQUE KEY uk_retry_source_event (event_type, source_event_id);
