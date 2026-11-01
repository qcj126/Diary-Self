# Redis 全局模型并发控制：设计、实现与验证

## 1. 背景与目标

`diary-ai` 可以部署多个实例。原有的 JVM `Semaphore` 只能限制单个实例的模型调用数；实例扩容后，所有实例的总调用数可能超过模型供应商或模型池的并发配额。

本次改造的目标是：按模型维度限制所有 `diary-ai` 实例的总并发，同时保留原有的任务租约、可靠 Outbox 和本地资源保护能力。

例如：

```text
QWEN37PLUS          最大并发 5
DEEPSEEKV4PRO0813   最大并发 10
```

这表示两个模型池彼此独立；总上限可以是 15，但不会因为某个模型占满而阻塞另一个模型。

## 2. 三种机制的职责边界

| 机制 | 解决的问题 | 不解决的问题 |
| --- | --- | --- |
| JVM 本地 `Semaphore` | 保护单个 `diary-ai` 实例的线程、连接和内存 | 不能限制多实例总并发 |
| Redis 分布式 permit | 限制同一模型池在所有实例上的总并发 | 不决定谁拥有任务、谁能写结果 |
| MySQL 任务租约 | 决定任务归属、故障接管、旧 Worker 防覆盖 | 不限制模型供应商总并发 |

Redis permit 不是分布式锁。锁会倾向于把所有模型调用串行化；permit 允许最多 N 个调用同时执行。

## 3. Redis permit 模型

### 3.1 Key 与成员

每个模型一个 ZSet：

```text
{key-prefix}:global-permit:model:{AIEnum}
```

例如：

```text
diary:dev:ai:global-permit:model:QWEN37PLUS
```

ZSet 规则：

```text
member = permitId = taskId:workerId:UUID
score  = permit 到期时间戳（毫秒）
```

### 3.2 获取、续期与释放

`RedisModelPermitManager` 使用 Lua 原子执行获取：

1. 用 Redis `TIME` 获得统一时钟；
2. 清理 score 小于等于当前时间的过期 permit；
3. 若当前 `permitId` 已存在，则续期并返回成功，支持网络响应丢失后的幂等重试；
4. 若有效 permit 数量达到模型上限，返回未获取；
5. 否则写入新的 `permitId` 与到期时间。

续期脚本只延长尚未过期、仍存在的 member，绝不复活已释放或已过期 permit。正常结束使用 ZSet `ZREM` 主动释放；Worker 崩溃来不及释放时，后续申请会清理过期 permit。

## 4. 配置

```yaml
diary:
  ai:
    limit:
      model-local-concurrency: 5
      local-permit-wait-ms: 1000
      global-model-permit:
        lease-seconds: 90
        renewal-interval-seconds: 20
        capacity-retry-delay-seconds: 10
        redis-unavailable-retry-delay-seconds: 30
        models:
          QWEN37PLUS:
            max-concurrency: 5
          DEEPSEEKV4PRO0813:
            max-concurrency: 10
```

约束：

```text
renewal-interval-seconds < lease-seconds
```

permit lease 与登录 token 的 TTL 完全无关，不能复用或修改登录 token 的过期策略。

## 5. Consumer 的资源获取顺序

`AiOutboxConsumer` 的正确顺序如下：

```text
校验消息
  → 查询任务并确认其可派发
  → 从 input_snapshot 解析 AIEnum
  → 获取 JVM 本地 permit
  → 获取 Redis 全局模型 permit
  → 抢占 MySQL 任务租约
  → 启动 MySQL 租约续期与 Redis permit 续期
  → 调用模型
  → finally 释放 Redis permit
  → finally 释放 JVM 本地 permit
```

关键不变量：

- Redis permit 获取失败时，任务还没有进入 `RUNNING`；
- Redis permit 已取得、MySQL 抢占失败时，外层 `finally` 必须释放 Redis permit；
- 成功、失败、线程中断、任务所有权确认失败都会进入 release；
- Redis release 失败时记录告警，permit 会在租约到期后自动回收。

## 6. 全局名额满与 Redis 不可用

| 情况 | 是否调用模型 | 任务状态 | 延迟 | 错误码 |
| --- | --- | --- | --- | --- |
| 模型池已满 | 否 | `RETRY_WAIT` | 10 秒 + 0～3 秒抖动 | `MODEL_CAPACITY_EXHAUSTED` |
| Redis 不可用 | 否 | `RETRY_WAIT` | 30 秒 + 0～3 秒抖动 | `REDIS_UNAVAILABLE` |

Consumer 在这两种情况下不会直接持续返回 RocketMQ `FAILURE`。它会在同一数据库事务中：

```text
条件更新 ai_task: PENDING / QUEUED → RETRY_WAIT
  +
插入未来时间可发送的 AI_TASK_RETRY Outbox
```

事务成功后才 ACK 原消息。事务失败则返回 `FAILURE`，保留原消息的恢复机会。

## 7. Outbox 发送确认竞态

Publisher 不能把“发送到 RocketMQ”和“更新 MySQL 为 SENT”放进同一个本地事务，因此存在窗口：

```text
Publisher 发送 AI_TASK_RETRY 给 Broker
  → Broker 立刻投递 Consumer
  → Consumer 读取到 task 仍为 RETRY_WAIT
  → Publisher 才确认 SENT，并将 RETRY_WAIT → QUEUED
```

若 Consumer 在第 3 步把消息误判为旧消息并 ACK，最终会出现：

```text
task = QUEUED
outbox = SENT
Broker 消息已 ACK
```

任务没有后续消息可消费，形成丢失唤醒。

修复策略：仅当收到的消息事件类型确实是 `AI_TASK_RETRY`，且任务仍为 `RETRY_WAIT` 时，Consumer 可以条件执行：

```text
RETRY_WAIT → QUEUED
```

再进入正常 permit 和租约流程。普通 `AI_TASK_CREATED` 的迟到重复消息不能做这一步，否则会绕过延迟时间。

## 8. Retry Outbox 的来源事件去重

### 8.1 问题

RocketMQ 是至少一次投递。Redis 长时间不可用时，迟到或重复的旧消息可能多次尝试安排下一轮 `AI_TASK_RETRY`，导致一条任务出现 Retry Outbox 分叉。

仅靠 `next_retry_time = 30 秒` 无法避免该问题：30 秒限制的是每条新 Retry Outbox 的发送时间，不是同一任务的全局去抖时间。

### 8.2 解决方案

`mq_outbox` 增加：

```text
source_event_id
```

含义：当前 Retry 是由哪条已消费消息触发。

数据库唯一约束：

```sql
UNIQUE KEY uk_retry_source_event (event_type, source_event_id)
```

其中 `event_id` 与 `source_event_id` 的职责不同：

```text
event_id        当前 Outbox 自己的唯一身份
source_event_id 触发当前 Retry 的来源消息身份
```

形成线性因果链：

```text
AI_TASK_CREATED(E1)
  → AI_TASK_RETRY(E2, source_event_id=E1)
  → AI_TASK_RETRY(E3, source_event_id=E2)
  → AI_TASK_RETRY(E4, source_event_id=E3)
```

同一来源事件 E2 被重复投递时，再次插入 `source_event_id=E2` 会触发唯一键冲突。服务将其识别为“该来源已安排过下一轮重试”，不再创建新的 Retry Outbox。

### 8.3 事务顺序

`AiTaskDispatchDeferralServiceImpl` 的事务顺序：

```text
1. 插入带 source_event_id 的下一条 Retry Outbox，作为唯一预约
2. 若唯一键冲突，返回 false：来源消息已处理过
3. 条件更新任务为 RETRY_WAIT
4. 若第 3 步失败，抛异常并回滚第 1 步，避免孤儿 Retry Outbox
```

来源事件 ID 为空的消息会被 Consumer 协议校验拒绝，避免去重键失效。

## 9. 数据库迁移

现有数据库必须执行一次迁移：

[`ai_task_outbox_retry_source_event_migration.sql`](../../diary-common/src/main/resources/sql/ai_task_outbox_retry_source_event_migration.sql)

新环境的基础建表定义也已更新：

[`mq.sql`](../src/main/java/diary/diaryai/entity/mq/mq.sql)

## 10. 已完成的验证

- 单任务正常完成，终态无 Redis permit 遗留；
- 将同模型上限临时设为 1，第二任务进入 `RETRY_WAIT`，随后成功；
- 验证容量满时产生 `AI_TASK_RETRY`，而不是 MQ 高频重试；
- Redis 不可用时得到 `REDIS_UNAVAILABLE`，不调用模型；
- Redis 恢复后任务最终成功，且只产生一次 `AI_COMPLETED`；
- 长时间 Redis 故障下，按 `source_event_id` 分组没有重复 Retry Outbox，因果链保持线性。

## 11. 后续可选增强

- 任务已进入终态时，取消尚未发送的未来 `AI_TASK_RETRY` Outbox，减少无效消息；
- 为 permit 续期明确丢失增加模型 SDK 取消策略；
- 增加自动化测试：容量满、Redis 异常、重复消息、发送确认竞态、Worker 崩溃；
- 增加 permit 指标：模型池使用数、获取失败数、自动过期回收数、Redis 异常数。
