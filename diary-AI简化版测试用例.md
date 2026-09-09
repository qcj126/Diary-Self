# diary-AI 简化版测试用例

> 适用范围：单实例 `diary-AI`，保留 Outbox + RocketMQ + MySQL 状态机，Redis 只用于提交限流。
>
> 用例形式：手工/联调测试，不要复制到 `src/test`。

## 1. 测试前提

- MySQL 已创建 `ai_task`、`ai_info`、`ai_nutrient`、`mq_outbox`。
- 存量库已按需执行 `ai_task_state_v4_simplify_migration.sql`。
- Redis、RocketMQ 5.x、Nacos、`diary-gateway`和 `diary-AI` 已启动。
- 通过网关调用，网关会从 JWT 生成 `X-Auth-User-Id`。不要在请求体中传 `userId`。
- 为每个用例保留对应的 `taskId`、`event_id`和 RocketMQ `messageId`。

## 2. 基础提交与查询

### AI-TC-001 正常提交任务

- 优先级：P0
- 操作：使用有效 JWT 调用 `POST /ai/tasks`，传入唯一 `clientRequestId`和合法营养分析参数。
- 预期 HTTP：`202 Accepted`，返回 `taskId`和 `PENDING`。
- 预期数据：
  - `ai_task` 只新增 1 行；
  - `user_id` 来自 `X-Auth-User-Id`；
  - `attempt_count=0`；
  - 同一事务新增 1 条 `AI_TASK_CREATED/NEW` Outbox。

### AI-TC-002 查询任务状态

- 优先级：P0
- 操作：调用 `GET /ai/tasks/{taskId}`。
- 预期：直接从 MySQL 返回最新状态；Redis 中不生成 `:task:` 缓存键。

### AI-TC-003 查询成功结果

- 优先级：P0
- 前置：任务已进入 `SUCCESS`。
- 操作：调用 `GET /ai/tasks/{taskId}/result`。
- 预期：返回 `200 OK`、模型信息和营养数据；`ai_info`、`ai_nutrient`与 `ai_task.ai_info_id` 关联一致。

### AI-TC-004 查询处理中结果

- 优先级：P1
- 前置：任务是 `PENDING/QUEUED/RUNNING/RETRY_WAIT`。
- 预期：结果接口返回 `202 Accepted`，响应体包含当前状态。

### AI-TC-005 跨用户查询拦截

- 优先级：P0
- 操作：用用户 B 的 JWT 查询用户 A 的 `taskId`。
- 预期：不返回用户 A 的任务或结果，数据库查询条件同时包含 `taskId + userId`。

## 3. 幂等与限流

### AI-TC-010 同一请求重复提交

- 优先级：P0
- 操作：同一用户使用相同 `clientRequestId` 和相同业务内容提交两次。
- 预期：两次返回同一 `taskId`；`ai_task`和初始 Outbox 都只有 1 条。
- 补充检查：Redis 中不生成 `:idem:` 键，正确性由 MySQL 唯一索引保证。

### AI-TC-011 幂等键冲突

- 优先级：P0
- 操作：同一用户复用 `clientRequestId`，但修改食材、用途或 `universalId`。
- 预期：抛出幂等冲突业务异常；不创建新任务或 Outbox。

### AI-TC-012 并发重复提交

- 优先级：P0
- 操作：并发发送 10 个完全相同的提交请求。
- 预期：仅 1 个插入成功，其余请求在唯一索引冲突后读取同一任务；所有成功响应的 `taskId` 一致。

### AI-TC-013 单用户提交限流

- 优先级：P0
- 操作：在一分钟内超过 `submit-per-user-per-minute`。
- 预期：超限请求在创建任务前被拒绝；Redis 只出现 `:submit:rate:` 键。

### AI-TC-014 Redis 不可用

- 优先级：P1
- 操作：停止 Redis 后分别执行状态查询和任务提交。
- 预期：
  - 状态/结果查询仍可直接访问 MySQL；
  - 新提交按当前 fail-closed 策略被拒绝，避免 Redis 故障时突破 AI 成本上限。

## 4. Outbox 可靠投递

### AI-TC-020 初始 Outbox 发送成功

- 优先级：P0
- 预期：`mq_outbox: NEW -> SENDING -> SENT`，写入 `broker_message_id/sent_time`；任务从 `PENDING -> QUEUED`。

### AI-TC-021 Broker 短暂不可用

- 优先级：P0
- 操作：提交任务后暂停 RocketMQ，再恢复。
- 预期：Outbox 按退避时间进入 `RETRY_WAIT`，Broker 恢复后转为 `SENT`；不丢任务。

### AI-TC-022 Outbox 重试耗尽

- 优先级：P1
- 操作：让任务投递 Outbox 持续发送失败直至超过 `outbox-max-retries`。
- 预期：Outbox 进入 `DEAD`；尚未执行的任务进入 `DEAD_LETTER`；同一事务写入 `AI_FAILED` 终态 Outbox。

### AI-TC-023 SENDING 超时

- 优先级：P1
- 操作：构造一条超过 `publisher-sending-timeout-seconds` 的 `SENDING` 记录。
- 预期：该记录被当作一次失败尝试，进入 `RETRY_WAIT` 或在超限时进入 `DEAD`。

### AI-TC-024 终态事件投递

- 优先级：P1
- 预期：成功任务产生 `AI_COMPLETED`，最终失败任务产生 `AI_FAILED`；Outbox 的 `event_id`、payload `eventId`、Tag 一致。

## 5. Consumer、重试与租约

### AI-TC-030 正常执行

- 优先级：P0
- 预期：任务 `QUEUED -> RUNNING -> SUCCESS`；`attempt_count` 只在抢占成功时加 1；结果与成功事件同事务提交。

### AI-TC-031 重复消息

- 优先级：P0
- 操作：对同一 `taskId` 重复投递相同消息。
- 预期：只有一条消息能原子抢占任务；模型、`ai_info`和 `ai_nutrient` 均不重复执行。

### AI-TC-032 可重试的模型异常

- 优先级：P0
- 操作：让模型调用第一次抛出非 `IllegalArgumentException`，后续恢复。
- 预期：`RUNNING -> RETRY_WAIT`，Listener 返回失败使 RocketMQ 重投；下一次抢占成功后执行并进入 `SUCCESS`。

### AI-TC-033 不可重试的输入异常

- 优先级：P0
- 操作：构造会抛出 `IllegalArgumentException` 的请求快照。
- 预期：任务直接进入 `FAILED`，写入 `AI_FAILED` Outbox，不再重试模型。

### AI-TC-034 执行次数耗尽

- 优先级：P0
- 预期：`attempt_count >= max_attempts` 时任务进入 `FAILED`，只产生一个有效终态结果。

### AI-TC-035 本地并发上限

- 优先级：P1
- 操作：同时提交超过 `model-local-concurrency` 的可执行消息。
- 预期：同时模型调用数不超过上限；未取得许可的消息交给 RocketMQ 后续重投。

### AI-TC-036 长时间调用续租

- 优先级：P1
- 操作：让模型调用时间超过租约的一个续租周期。
- 预期：`lease_until` 持续向后更新；任务不被 Recovery 误判为过期。

### AI-TC-037 进程崩溃后恢复 RUNNING

- 优先级：P0
- 操作：任务进入 `RUNNING` 后强制终止 `diary-AI`，等待租约过期后重启。
- 预期：Recovery 将过期任务改为 `RETRY_WAIT` 并写入 `AI_TASK_RETRY` Outbox；最终重新执行或在次数耗尽时进入 `FAILED`。

### AI-TC-038 等待态不自动补发

- 优先级：P1
- 操作：构造长时间停留的 `PENDING/QUEUED/RETRY_WAIT`。
- 预期：Recovery Job 不生成额外补发世代，数据库不再需要 `recovery_count`。通过 Outbox 状态、Consumer Lag 和 DLQ 定位原因。

## 6. 多模型工厂

### AI-TC-040 现有 Qwen Plus 注册

- 优先级：P0
- 预期：应用启动时 `AIFactory` 注册 Qwen Plus 的 code，消息可根据 `aiType` 获取正确实现。

### AI-TC-041 第二个模型注册

- 优先级：P0
- 前置：新模型实现 `InvokeAIService`并声明唯一 code。
- 预期：启动日志显示两个模型的注册映射；两种 `aiType` 分别路由到对应实现。

### AI-TC-042 模型 code 重复

- 优先级：P0
- 操作：临时让两个实现返回相同 code。
- 预期：应用应在启动阶段拒绝重复注册，不应静默覆盖。若当前尚未拒绝，记为第二模型接入前必修项。

### AI-TC-043 不支持的模型 code

- 优先级：P1
- 预期：工厂抛出明确的参数异常，不调用任何模型。

## 7. 数据库迁移与回归

### AI-TC-050 V4 迁移

- 优先级：P0
- 操作：备份数据库，确认已执行 V3，再执行 `ai_task_state_v4_simplify_migration.sql`。
- 预期：`recovery_count` 和 `idx_ai_task_status_update` 被删除；存量任务、唯一索引、租约索引及 Outbox 数据不受影响。

### AI-TC-051 新库建表

- 优先级：P1
- 操作：使用最新 `ai.sql` 建表。
- 预期：`ai_task` 不包含 `recovery_count`；应用可以正常插入、查询和迁移状态。

## 8. 验收记录模板

| 用例 ID | 结果 | taskId/eventId | 实际现象 | 日志/截图 | 执行人 | 日期 |
| --- | --- | --- | --- | --- | --- | --- |
| AI-TC-001 | 待执行 |  |  |  |  |  |
| AI-TC-010 | 待执行 |  |  |  |  |  |
| AI-TC-020 | 待执行 |  |  |  |  |  |
| AI-TC-030 | 待执行 |  |  |  |  |  |
| AI-TC-037 | 待执行 |  |  |  |  |  |
| AI-TC-041 | 待执行 |  |  |  |  |  |
