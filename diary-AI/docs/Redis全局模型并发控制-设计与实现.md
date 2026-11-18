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

保留两类错误的独立日志和错误码，但不再创建第二条“容量延期 Outbox”：

| 情况 | 是否调用模型 | Consumer结果 | 错误码 |
| --- | --- | --- | --- |
| 模型池已满 | 否 | `FAILURE`，交给RocketMQ有限重投 | `MODEL_CAPACITY_EXHAUSTED` |
| Redis 不可用 | 否 | `FAILURE`，交给RocketMQ有限重投 | `REDIS_UNAVAILABLE` |

这是最多三个实例、约100 QPS学习场景下的最小实现。代价是持续满载时会增加MQ重投；只有实际观察到大量重试或DLQ后，才值得恢复独立的容量延期队列。

## 7. 重复消息与任务抢占

RocketMQ至少一次投递可能产生重复消息。`claimForExecution` 使用任务状态、用户、请求ID和数据库CAS抢占执行权；抢占失败的消息直接ACK，真正的Worker继续执行。任务租约过期后，由恢复任务重新创建 `AI_TASK_RETRY` Outbox。

历史表中的 `source_event_id` 和唯一索引继续保留以兼容已有数据，但当前主链路不再为容量不足创建因果重试Outbox，不需要回滚数据库迁移。

## 8. 已完成的验证

- 单任务正常完成，终态无 Redis permit 遗留；
- 将同模型上限临时设为1时，后续任务不调用模型并返回消费失败；
- Redis不可用时记录 `REDIS_UNAVAILABLE`，不调用模型；
- Redis恢复或模型名额释放后，由RocketMQ重投继续处理任务；
- 重复消息只能有一个实例通过数据库抢占并调用模型。

## 9. 后续可选增强

- 为 permit 续期明确丢失增加模型 SDK 取消策略；
- 当容量不足导致大量MQ重试或DLQ时，再引入专用延迟消息；
- 增加自动化测试：容量满、Redis异常、重复消息、Worker崩溃；
- 增加 permit 指标：模型池使用数、获取失败数、自动过期回收数、Redis 异常数。
