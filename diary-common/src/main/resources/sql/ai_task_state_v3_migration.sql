-- AI task/outbox 状态机 V3 存量数据库迁移脚本。
-- 1. request_hash 用于识别“同一幂等键、不同请求内容”。
--    存量数据保持 NULL，首次重复提交时应用会解析 input_snapshot、进行语义比较后安全回填。
-- 2. recovery_count 是等待态消息的显式补发代数，不再从 Outbox 历史数和执行次数反推。

ALTER TABLE ai_task
    ADD COLUMN request_hash VARCHAR(64) NULL
        COMMENT '规范化请求内容SHA-256' AFTER client_request_id,
    ADD COLUMN recovery_count INT NOT NULL DEFAULT 0
        COMMENT '等待态消息补发次数' AFTER max_attempts;

-- SENDING 超时扫描和 SENT 分批清理共用，避免 Outbox 增长后全表扫描。
CREATE INDEX idx_status_update
    ON mq_outbox (status, update_time, id);
