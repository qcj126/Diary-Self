-- diary-AI 单实例简化迁移脚本。
--
-- V4 取消等待态自动补发，PENDING/QUEUED/RETRY_WAIT 不再维护独立的
-- recovery_count。Outbox 负责 Broker 前的可靠投递，RocketMQ 负责消费重投，
-- Recovery Job 只处理租约过期的 RUNNING 任务。
--
-- 仅对已执行 ai_task_state_v3_migration.sql 的存量数据库执行。

ALTER TABLE ai_task
    DROP COLUMN recovery_count,
    DROP INDEX idx_ai_task_status_update;
