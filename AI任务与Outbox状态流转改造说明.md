# AI 任务与 Outbox 状态流转改造说明

## 1. Task 与 Outbox 终态一致性

### 改前

- Consumer 在部分“执行次数耗尽”分支只把 task 更新成 `FAILED`，没有创建 `AI_FAILED` Outbox。
- `AI_TASK_CREATED` 或 `AI_TASK_RETRY` Outbox 进入 `DEAD` 后，task 仍停留在 `PENDING/QUEUED/RETRY_WAIT`。

### 存在的问题

- 下游可能看到“数据库已经失败，但永远没有失败事件”。
- 投递彻底失败的任务没有业务终态，会被客户端永久轮询。

### 改后

- `failExhaustedTask` 统一执行 `FAILED + AI_FAILED Outbox`，Consumer 和 Recovery 共用同一事务入口。
- 任务投递 Outbox 重试耗尽时，只把仍处于等待态的 task CAS 更新成 `DEAD_LETTER`，并追加失败事件。

### 解决效果

- task 终态和终态事件要么一起提交，要么一起回滚。
- 已经 `RUNNING/SUCCESS/FAILED` 的任务不会被旧 Outbox 覆盖。

## 2. 失败事件协议

### 改前

- 失败 Outbox 使用新的 `event_id`，payload 却复用原任务消息的旧 `eventId`。
- `AI_COMPLETED` 与 `AI_FAILED` 共用 `AI_COMPLETED` Tag。

### 存在的问题

- 下游无法基于 eventId 正确去重。
- 按 `AI_FAILED` Tag 订阅的消费者收不到失败事件。

### 改后

- 完成/失败事件统一使用 `AiTaskEventDto`。
- Outbox `event_id` 与 payload `eventId` 使用同一个值。
- 配置拆分为 `completed-tag` 和 `failed-tag`，默认分别为 `AI_COMPLETED`、`AI_FAILED`。

Nacos 中原来的 `event-tag` 需要调整为：

```yaml
diary:
  ai:
    rocketmq:
      completed-tag: AI_COMPLETED
      failed-tag: AI_FAILED
```

## 3. 等待态任务恢复

### 改前

Recovery Job 只扫描租约过期的 `RUNNING`。如果消息在执行前因本地并发满、数据库异常或 Broker 重投耗尽进入 DLQ，task 会永久停在等待态。

### 改后

- `ai_task` 增加 `update_time`。
- Recovery Job 同时扫描长期停留的 `PENDING/QUEUED/RETRY_WAIT`。
- 存在活跃 Outbox 时不重复补发；无活跃 Outbox 时通过 version CAS 创建恢复消息。
- 连续生成的恢复消息始终未被 Consumer 抢占时，task 收敛到 `DEAD_LETTER`。

相关配置：

```yaml
diary:
  ai:
    task:
      waiting-recovery-seconds: 600
      waiting-max-recovery-messages: 3
```

## 4. RUNNING 租约续期

### 改前

租约只在 Consumer 抢占时写一次。模型调用超过租约后，Recovery 可能启动第二次模型调用。

### 改后

执行期间按租约的三分之一周期续期；续期必须匹配 `taskId + workerId + versionId`，且不递增状态版本。

## 5. 用户归属与缓存

### 改前

- AI 服务固定使用 `userId=10000`。
- 状态和结果只按 taskId 查询。
- 状态缓存只按 taskId 命中，无法校验 owner。

### 改后

- JWT 增加 `user_id` claim，网关清除外部伪造身份头后写入 `X-Auth-User-Id`。
- AI 接口使用 `taskId + userId` 查询。
- Redis task 缓存条目保存 owner userId，不匹配时强制回查数据库。

> 部署提示：旧 JWT 没有 `user_id`，发布后需要用户重新登录。存量固定为 `user_id=10000` 的 AI 任务是否迁移，需要根据真实用户映射单独处理。

## 6. 数据库迁移

新库使用更新后的 `ai.sql`。存量库执行：

`diary-common/src/main/resources/sql/ai_task_state_v2_migration.sql`

迁移增加：

- `ai_task.update_time`
- `idx_ai_task_status_lease(status, lease_until, id)`
- `idx_ai_task_status_update(status, update_time, id)`

## 7. 其他行为调整

- Redis task 缓存改为 fail-open，不再在数据库已经受理任务后向客户端返回假 500。
- 终态 `SUCCESS/FAILED/CANCELLED/DEAD_LETTER` 的结果查询统一返回 HTTP 200；处理中返回 HTTP 202。
- 单条 Outbox 状态落库失败不再中断整个发布批次。
- `maxRetries` 语义调整为“首次发送之外允许的重试次数”。
