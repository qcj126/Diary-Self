-- AI task/outbox 状态机 V2 一次性迁移脚本（存量数据库执行，新库直接使用 ai.sql）。
--
-- 改前：ai_task 没有 update_time，无法可靠判断 PENDING/QUEUED/RETRY_WAIT 停留时长；
--       过期 RUNNING 与等待态扫描也缺少组合索引，数据增大后容易全表扫描。
-- 改后：增加状态更新时间与两组恢复索引，为等待态对账和 RUNNING 租约恢复提供稳定查询条件。

ALTER TABLE ai_task
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次状态更新时间' AFTER finish_time;

CREATE INDEX idx_ai_task_status_lease
    ON ai_task (status, lease_until, id);

CREATE INDEX idx_ai_task_status_update
    ON ai_task (status, update_time, id);
