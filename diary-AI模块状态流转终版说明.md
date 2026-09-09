# diary-AI 模块状态流转终版说明

> 文档状态：简化版终版（2026-09-09）
>
> 适用范围：`diary-AI` 单应用实例
>
> 核心边界：MySQL 是任务状态和幂等事实的唯一权威；Redis 只用于提交限流

## 1. 结论

`diary-AI` 使用两套状态，两者不可互相代替：

- `ai_task.status` 表示 AI 任务的业务执行事实。
- `mq_outbox.status` 表示某个事件到 RocketMQ 的投递事实。

当前采用：

```text
本地事务 + Outbox + RocketMQ 至少一次投递 + 数据库条件更新
```

这是最终一致性模型，不是全局分布式事务。

## 2. 当前版本边界

- `diary-AI` 按单实例运行。
- 保留 `worker_id`、`lease_until`、`version_id` 和本地并发限制，用于防止重复消息重复调用模型，以及恢复进程崩溃后的 RUNNING 任务。
- 不使用分布式调度锁、全局模型信号量和多实例缓存一致性。
- 不缓存任务状态，不缓存幂等映射。
- 不再扫描 `PENDING/QUEUED/RETRY_WAIT` 生成独立的自动补发世代。
- `AI应用RocketMq-版本3.md` 是历史储备方案，不是当前代码目标。

## 3. 组件职责

| 组件 | 职责 |
| --- | --- |
| `DiaryAIController` | 提交、查询状态、查询结果；接收网关注入的 `X-Auth-User-Id` |
| `AiTaskApplicationServiceImpl` | 校验、MySQL 幂等、提交限流、创建任务 |
| `AiTaskCommandServiceImpl` | 任务与 Outbox 的事务写入，成功/失败状态迁移 |
| `AiOutboxPublisher` | 扫描、领取、发送和恢复超时 Outbox |
| `AiOutboxConsumer` | 协议校验、本地并发限制、任务抢占、ACK/重试决策 |
| `AiTaskExecutor` | 解析输入快照，通过 `AIFactory` 获取模型策略 |
| `AiTaskLeaseRenewer` | 执行中按租约的 1/3 周期续期 |
| `AiTaskRecoveryJob` | 只恢复租约过期的 `RUNNING` |
| `AiSubmitRateLimiter` | Redis 每用户每分钟提交限流 |
| `DiaryAiMapper.xml` | 用前置状态、所有权和版本条件实现状态迁移 |

## 4. ai_task 状态

| 状态 | 终态 | 含义 |
| --- | --- | --- |
| `PENDING` | 否 | 任务与初始 Outbox 已提交，尚未确认 Broker 接收 |
| `QUEUED` | 否 | 任务投递 Outbox 已收到 Broker 成功回执 |
| `RUNNING` | 否 | Consumer 已原子抢占，模型正在执行 |
| `RETRY_WAIT` | 否 | 可重试错误或租约过期，等待 MQ 重投/恢复消息 |
| `SUCCESS` | 是 | 结果、任务成功状态和完成事件 Outbox 已提交 |
| `FAILED` | 是 | 永久错误或执行次数耗尽 |
| `DEAD_LETTER` | 是 | 任务投递 Outbox 重试耗尽，任务尚未开始执行 |
| `CANCELLED` | 是 | 枚举预留，当前没有取消链路 |

## 5. mq_outbox 状态

| 状态 | 含义 |
| --- | --- |
| `NEW` | 本地事务已创建，待发送 |
| `SENDING` | Publisher 已领取，发送结果尚未确认 |
| `RETRY_WAIT` | 上次发送失败，等待下次发送 |
| `SENT` | Broker 已确认收到 |
| `DEAD` | 发送重试已耗尽，保留供排障和人工处理 |

`SENT` 只表示 Broker 收到，不表示 Consumer 或 AI 模型已处理成功。

## 6. 事件类型

| 事件 | Topic/Tag | 作用 |
| --- | --- | --- |
| `AI_TASK_CREATED` | 任务 Topic/任务 Tag | 首次分发任务 |
| `AI_TASK_RETRY` | 任务 Topic/任务 Tag | 恢复租约过期的 RUNNING |
| `AI_COMPLETED` | 事件 Topic/`AI_COMPLETED` | 向下游声明成功终态 |
| `AI_FAILED` | 事件 Topic/`AI_FAILED` | 向下游声明失败终态 |

## 7. 核心流转

```text
创建：
  (插入 ai_task=PENDING + 插入 AI_TASK_CREATED Outbox=NEW) 同事务

投递：
  Outbox NEW/RETRY_WAIT -> SENDING -> SENT
  task PENDING/RETRY_WAIT -> QUEUED

执行：
  task PENDING/QUEUED/RETRY_WAIT -> RUNNING
  可重试错误：RUNNING -> RETRY_WAIT
  永久错误/次数耗尽：RUNNING -> FAILED + AI_FAILED Outbox
  成功：RUNNING -> SUCCESS + 结果 + AI_COMPLETED Outbox

崩溃恢复：
  过期 RUNNING -> RETRY_WAIT + AI_TASK_RETRY Outbox

投递死亡：
  任务 Outbox -> DEAD
  尚未执行的 task -> DEAD_LETTER + AI_FAILED Outbox
```

## 8. 幂等性

提交幂等依靠：

```text
UNIQUE(user_id, client_request_id)
+ request_hash
+ DuplicateKeyException 后重读
```

- 同一用户、同一幂等键、相同内容：返回原任务。
- 同一用户、同一幂等键、不同内容：返回幂等冲突。
- 不使用 Redis 幂等缓存。

执行幂等依靠：

- 原子抢占只能影响 1 行。
- 结果提交必须匹配 `status=RUNNING + worker_id + version_id`。
- `ai_nutrient.ai_task_id` 唯一索引作为最后的结果去重保护。

## 9. 重试计数边界

| 计数 | 所属 | 含义 |
| --- | --- | --- |
| `attempt_count` | `ai_task` | 成功抢占并实际进入模型执行的次数 |
| `retry_count` | 单条 `mq_outbox` | 该事件到 Broker 的发送失败次数 |
| RocketMQ delivery attempt | Broker | Consumer 重投次数，不写入上述两个字段 |

`recovery_count` 已移除。

## 10. 租约和 Recovery

- 抢占成功时写入 `worker_id`和 `lease_until`。
- 长模型调用期间，单线程调度器按租约的 1/3 周期续期。
- 续期必须匹配 `taskId + workerId + versionId`。
- Recovery 只扫描租约过期的 `RUNNING`。
- 等待态长时间不变时，检查 Outbox、Consumer Lag 和 DLQ，不自动生成无法判断的第三层补发。

## 11. 终态事务边界

成功事务：

```text
ai_info + ai_nutrient + ai_task=SUCCESS + AI_COMPLETED Outbox=NEW
```

失败事务：

```text
ai_task=FAILED/DEAD_LETTER + AI_FAILED Outbox=NEW
```

任一事务内操作失败都整体回滚，避免产生“有终态无事件”或“有结果无成功状态”。

## 12. 查询边界

- 任务状态和结果直接查 MySQL。
- Mapper 必须使用 `taskId + userId`，防止跨用户读取。
- `userId` 来自网关注入的 `X-Auth-User-Id`，不接受请求体中的身份字段。
- 处理中的结果查询返回 HTTP 202；所有终态返回 HTTP 200，业务结果由响应体 `status` 表达。

## 13. 已知边界

- `CANCELLED` 只有枚举，尚无取消接口。
- `diary-ai-event` 的终态事件仍需由 `diary-notify` 等下游完成业务闭环。
- 等待态消息进入 DLQ 后不再由应用盲目补发，需要监控和人工重放流程。
- 当前单实例调度任务没有分布式锁，这是明确边界。
- 实际配置从 Nacos 加载，仓库中的 `src/main/resources/a` 只是参考配置。

## 14. 排障索引

| 现象 | 首先查看 |
| --- | --- |
| `PENDING` 很久 | 初始 Outbox 状态、Publisher、RocketMQ 连接 |
| `QUEUED` 很久 | Consumer 是否在线、Consumer Lag、DLQ |
| `RUNNING` 很久 | `lease_until` 是否续期、模型请求是否卡死 |
| `RETRY_WAIT` 很久 | RocketMQ 重投和 DLQ、`attempt_count` |
| `DEAD_LETTER` | 任务 Outbox `last_error` |
| `SUCCESS/FAILED` 但下游未收到 | 终态 Outbox 状态和下游消费日志 |

## 15. 维护规则

修改状态机时必须同时核对：

1. `AiTaskStatusEnum` 和 `OutboxStatusEnum`。
2. `DiaryAIMapper.xml` 的前置状态、所有权和版本条件。
3. `AiTaskCommandServiceImpl` 中的事务边界。
4. `AiOutboxServiceImpl` 的投递死亡处理。
5. `AiTaskRecoveryJob` 的扫描范围。
6. `AI应用RocketMq-版本2-实操手册.md`。
7. `diary-AI简化版测试用例.md`。

任务状态只表达业务执行，Outbox 状态只表达单条事件投递。任何异步回调都不得无条件覆盖新状态。
