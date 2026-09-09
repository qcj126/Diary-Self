# AI 应用 RocketMQ——版本 2 实操手册

> 版本定位：单实例微服务简化版
>
> 适用模块：`diary-AI`
>
> 当前实现：Spring Boot + MySQL + RocketMQ 5.x + Outbox + Redis 限流 + Nacos
>
> 不包含：多实例调度锁、全局信号量、多实例缓存一致性和分布式 Outbox 批量领取

## 1. 本版目标

`diary-AI` 的核心问题是：模型调用慢、会失败、可能重复投递，而且每次调用都可能产生成本。版本 2 只保留解决这些真实问题所必需的结构：

1. HTTP 只受理任务，不同步等待模型。
2. MySQL 保存任务真实状态和幂等事实。
3. Outbox 解决“创建任务 + 发送 MQ”的双写一致性。
4. RocketMQ 异步分发任务并提供消费重投。
5. 原子抢占、租约和版本号防止重复消息重复调用模型。
6. 本地 `Semaphore` 限制单进程模型并发。
7. Redis 只负责提交限流，不再承担幂等缓存和任务状态缓存。
8. 保留 `InvokeAIService + AIFactory`，支持第二个大模型。

## 2. 为什么做简化

原实现存在三类重复保障：

- Redis 幂等缓存命中后仍必须查 MySQL，没有减少数据库读取。
- 任务状态 Cache-Aside 需要在 Publisher、Consumer、Recovery 和事务回调中分散失效。
- 等待态自动补发与 Outbox 投递重试、RocketMQ 消费重投形成第三层消息恢复。

简化后的责任边界是：

| 故障阶段 | 唯一主要机制 |
| --- | --- |
| 任务落库与消息创建 | MySQL 本地事务 + Outbox |
| Outbox 到 Broker | Publisher 有界退避重试 |
| Broker 到 Consumer | RocketMQ 消费重投 / DLQ |
| 模型执行失败 | `attempt_count/max_attempts` |
| 进程崩溃或模型卡死 | RUNNING 租约 + Recovery Job |
| 重复提交 | MySQL 唯一索引 + `request_hash` |
| 请求过载 | Redis 固定窗口限流 + 本地信号量 |

## 3. 当前架构

```text
Client
  -> diary-gateway
       -> JWT 校验
       -> 注入 X-Auth-User-Id
  -> diary-AI Controller
       -> MySQL: ai_task + mq_outbox（同一事务）
       -> 202 + taskId

AiOutboxPublisher
  -> 扫描 NEW / RETRY_WAIT Outbox
  -> RocketMQ diary-ai-task

AiOutboxConsumer
  -> 本地并发许可
  -> 原子抢占 ai_task
  -> AIFactory 选择模型
  -> 调用模型
  -> 成功：结果 + SUCCESS + AI_COMPLETED Outbox
  -> 失败：RETRY_WAIT 或 FAILED + AI_FAILED Outbox

AiTaskRecoveryJob
  -> 只扫描租约过期的 RUNNING
  -> RETRY_WAIT + AI_TASK_RETRY Outbox
```

`diary-AI` 仍是一个单实例微服务。MySQL、Redis、Nacos 和 RocketMQ 是外部基础设施，不表示 AI 服务已进入多实例分布式版本。

## 4. 代码目录与职责

```text
diary-AI/src/main/java/diary/diaryai
├── controller
│   └── DiaryAIController.java
├── executor
│   └── AiTaskExecutor.java
├── factory
│   └── AIFactory.java
├── guard
│   ├── LocalAiConcurrencyGuard.java
│   └── AiTaskLeaseRenewer.java
├── idempotency
│   └── AiRequestFingerprint.java
├── impl
│   ├── AiTaskApplicationServiceImpl.java
│   ├── AiTaskCommandServiceImpl.java
│   ├── AiTaskQueryServiceImpl.java
│   ├── AiOutboxServiceImpl.java
│   └── AiTaskRecoveryServiceImpl.java
├── outbox
│   ├── AiOutboxPublisher.java
│   ├── AiOutboxConsumer.java
│   └── AiOutboxMaintenanceJob.java
├── recovery/job
│   └── AiTaskRecoveryJob.java
├── redis
│   ├── AiRedisKeyFactory.java
│   └── AiSubmitRateLimiter.java
└── strategy
    ├── service/InvokeAIService.java
    └── nutrientanlalyze/InvokeQwenPlus.java
```

已删除的组件：

- `AiIdempotencyCacheService` 及实现。
- `AiTaskCacheService` 及实现。
- 为缓存失效服务的 `TaskRecoveredEvent/TaskCacheEvictListener`。
- 等待态扫描、`recovery_count` 和对应 Mapper。
- 四个全量注释、签名已过时的模型占位类；第二模型按第 15 节新建真实实现。

## 5. Maven 依赖

`diary-AI` 直接依赖：

- `diary-common`：DTO、PO、枚举和通用响应。
- `diary-utils`：主键、JSON 等通用工具。
- `diary-config`：日志切面等配置。
- Spring Web、MyBatis、MySQL、Redis、Nacos、RocketMQ、Actuator 和 Prometheus。

不再依赖 `diary-file`。AI 服务不应通过另一个业务微服务间接获得 `diary-utils/diary-config`。

## 6. 配置

`application.yaml` 只负责导入 Nacos 配置。下面内容应写入 `diary-AI.yaml`，仓库中的 `src/main/resources/a` 只能作为参考：

```yaml
spring:
  application:
    name: diary-ai

server:
  port: 8807

diary:
  ai:
    rocketmq:
      task-topic: diary-ai-task
      task-tag: QWEN_PLUS_NUTRIENT
      task-consumer-group: diary-ai-qwen-plus-worker-v2
      event-topic: diary-ai-event
      completed-tag: AI_COMPLETED
      failed-tag: AI_FAILED
      publisher-batch-size: 20
      publisher-interval-ms: 1000
      publisher-sending-timeout-seconds: 60
      outbox-max-retries: 10
      sent-retention-days: 7
      cleanup-batch-size: 500
    task:
      max-attempts: 3
      execution-lease-seconds: 330
      recovery-interval-ms: 30000
      recovery-batch-size: 50
    redis:
      key-prefix: diary:dev:ai
    limit:
      submit-per-user-per-minute: 10
      model-local-concurrency: 2
      local-permit-wait-ms: 1000

rocketmq:
  producer:
    endpoints: 192.168.101.128:7081
  push-consumer:
    endpoints: 192.168.101.128:7081
```

已废弃的配置项：

```text
diary.ai.cache.*
diary.ai.task.waiting-recovery-seconds
diary.ai.task.waiting-max-recovery-messages
```

Redis 中当前只应出现类似下列键：

```text
diary:dev:ai:submit:rate:{userId}:{epochMinute}
```

## 7. 用户身份边界

请求体不再接收 `userId`。身份链路固定为：

```text
JWT user_id
  -> diary-gateway 校验签名与会话
  -> 删除客户端伪造的 X-Auth-User-Id
  -> 注入可信 X-Auth-User-Id
  -> diary-AI Controller
  -> Service/Mapper 始终使用 taskId + userId
```

提交、状态查询和结果查询都必须通过网关访问。开发环境直连 `diary-AI` 时，需要显式提供测试用身份头。

## 8. 数据模型

### 8.1 ai_task

| 字段 | 作用 |
| --- | --- |
| `id` | 任务 ID |
| `user_id` | 数据归属 |
| `client_request_id` | 客户端幂等键 |
| `request_hash` | 防止同一幂等键复用于不同请求 |
| `task_type` | 任务类型/Tag |
| `status` | 业务状态 |
| `input_snapshot` | 稳定输入快照 |
| `attempt_count/max_attempts` | 模型实际执行次数和上限 |
| `worker_id/lease_until` | 当前执行权及过期时间 |
| `version_id` | 状态迁移乐观锁 |
| `error_code/error_message` | 最近一次错误 |
| `queue_time/start_time/finish_time` | 关键时间点 |

`recovery_count` 已删除。消息重投由 RocketMQ 处理，不再在任务表中另建一个等待态补发代数。

### 8.2 mq_outbox

| 字段 | 作用 |
| --- | --- |
| `event_id` | 全局事件 ID，并写入 payload |
| `aggregate_id` | 关联 `ai_task.id` |
| `event_type/topic/tag` | 路由信息 |
| `payload/schema_version` | 消息内容和协议版本 |
| `status` | `NEW/SENDING/RETRY_WAIT/SENT/DEAD` |
| `retry_count/max_retries` | Broker 前发送重试 |
| `next_retry_time` | 下次可发送时间 |
| `version_id` | Outbox 领取与回写的乐观锁 |

## 9. 数据库迁移

新库直接使用最新：

```text
diary-common/src/main/java/diary/common/entity/ai/ai.sql
diary-common/src/main/java/diary/common/entity/mq/mq.sql
```

已执行 V3 的存量库执行：

```text
diary-common/src/main/resources/sql/ai_task_state_v4_simplify_migration.sql
```

V4 删除 `ai_task.recovery_count` 和不再被查询使用的 `idx_ai_task_status_update`。执行前应先备份，并确认当前数据库已包含该列与索引。如果存量库未执行 V2/V3，不要执行 V4。

## 10. 任务状态机

```text
PENDING
  -> QUEUED       任务 Outbox 得到 Broker 成功回执
  -> RUNNING      Broker 先投递，Consumer 抢先执行
  -> DEAD_LETTER  任务 Outbox 最终投递失败

QUEUED / RETRY_WAIT
  -> RUNNING      Consumer 原子抢占

RUNNING
  -> SUCCESS      结果、状态和 AI_COMPLETED Outbox 同事务提交
  -> RETRY_WAIT   可重试的执行失败或租约过期恢复
  -> FAILED       永久错误或执行次数耗尽
```

`CANCELLED` 是预留枚举，当前没有取消接口和状态迁移。

## 11. 提交链路

### 11.1 请求校验

Controller 从 `X-Auth-User-Id` 获得用户，Service 校验：

- `clientRequestId`
- `aiType`
- `aiApplication`
- `flag`
- `materials`
- `universalId`

### 11.2 MySQL 幂等

1. 根据 `(user_id, client_request_id)` 查询已有任务。
2. 已有任务的 `request_hash` 相同则返回原 `taskId`。
3. 幂等键相同但内容不同则返回冲突。
4. 查询后的并发窗口由唯一索引兜底，捕获 `DuplicateKeyException` 后重读原任务。

不再读写 Redis 幂等键。

### 11.3 提交限流

Redis Lua 脚本使用 `INCR + EXPIRE` 实现每用户每分钟计数。Redis 异常时当前采用 fail-closed：拒绝新 AI 提交，防止限流失效后放大模型费用。

### 11.4 任务与初始 Outbox

`createTaskAndOutbox()` 在一个本地事务中插入：

- `ai_task=PENDING`
- `mq_outbox=AI_TASK_CREATED/NEW`

任一插入失败时整体回滚。

## 12. Outbox Publisher

Publisher 每次执行：

1. 恢复超时的 `SENDING`。
2. 扫描已到发送时间的 `NEW/RETRY_WAIT`。
3. 用 `id + status + version_id` 将单条记录抢占为 `SENDING`。
4. 同步发送到 RocketMQ。
5. 成功后写入 `SENT` 和 `broker_message_id`。
6. 任务投递事件成功时，将尚未执行的任务改为 `QUEUED`。
7. 失败时按有界退避写入 `RETRY_WAIT`，超限后进入 `DEAD`。

`max_retries` 不包含首次发送，总尝试次数为 `1 + max_retries`。

Outbox 进入 `DEAD` 后：

- 如果是 `AI_TASK_CREATED/AI_TASK_RETRY` 且任务仍未执行，任务进入 `DEAD_LETTER`并创建 `AI_FAILED` Outbox。
- 如果是终态事件，不回退已经完成的业务状态。

## 13. Consumer 与模型执行

Consumer 处理顺序：

1. 解析并校验消息协议。
2. 获取本地 `Semaphore`。
3. 生成本次执行 `workerId`。
4. 使用一条条件 UPDATE 原子抢占任务。
5. 重读任务并确认 `workerId`所有权。
6. 启动租约续期。
7. 根据 `aiType` 从 `AIFactory` 获取模型实现。
8. 调用模型并提交结果。
9. 关闭续租句柄并释放本地并发许可。

抢占 SQL 只允许：

- `PENDING/QUEUED/RETRY_WAIT`
- 租约已过期的 `RUNNING`
- `attempt_count < max_attempts`

重复消息遇到正在执行或已终态的任务时直接 ACK，不重复调用模型。

## 14. 成功、失败与恢复

### 14.1 成功事务

同一事务完成：

- 插入 `ai_info`。
- 插入 `ai_nutrient`。
- 按 `workerId + versionId` 将任务改为 `SUCCESS`。
- 创建 `AI_COMPLETED` Outbox。

旧 Worker 已失去租约所有权时，状态 UPDATE 为 0，整个结果事务回滚。

### 14.2 失败事务

- `IllegalArgumentException` 视为永久错误，直接 `FAILED + AI_FAILED Outbox`。
- 已达 `max_attempts` 时执行 `FAILED + AI_FAILED Outbox`。
- 其他异常改为 `RETRY_WAIT`，Listener 返回失败让 RocketMQ 重投。

### 14.3 RUNNING 恢复

Recovery Job 只扫描：

```sql
status = 'RUNNING' AND lease_until < CURRENT_TIMESTAMP
```

- 尚有执行次数：`RUNNING -> RETRY_WAIT`，同事务创建 `AI_TASK_RETRY` Outbox。
- 执行次数耗尽：`RUNNING -> FAILED`，同事务创建 `AI_FAILED` Outbox。

不再定时扫描 `PENDING/QUEUED/RETRY_WAIT` 生成额外补发世代。长时间等待时应查看：

- 对应 Outbox 是否 `NEW/RETRY_WAIT/DEAD`。
- RocketMQ Consumer Lag。
- RocketMQ DLQ。
- Consumer 和数据库日志。

## 15. 接入第二个大模型

### 15.1 同一营养分析任务下切换供应商

新建实现：

```java
@Service
public class InvokeSecondModel implements InvokeAIService {
    @Override
    public Integer getCode() {
        return 2; // 必须全局唯一
    }

    @Override
    public void getAiResultAndSave(
            Object data,
            Long taskId,
            Long userId,
            String workerId,
            Integer versionId) {
        // 1. 解析快照
        // 2. 调用第二个模型
        // 3. 调用 AiTaskCommandService.processData(...)
    }
}
```

Spring 会把所有 `InvokeAIService` 注入 `AIFactory`。工厂使用 `putIfAbsent` 注册 code，重复 code 会让应用启动失败，避免静默覆盖模型。

### 15.2 接入注意事项

- `getCode()` 必须与对外 `aiType` 约定一致。
- 不要在策略类内直接修改 `ai_task`。
- 成功结果统一经过 `AiTaskCommandService.processData()` 提交。
- 策略抛出 `IllegalArgumentException` 代表请求本身永久无效；网络超时、限流和服务端异常应抛其他异常以进入有界重试。
- 两个模型共享本地并发上限。如果各模型配额不同，再将 `LocalAiConcurrencyGuard` 扩展为按 model code 分组，不要现在提前做分布式信号量。
- 如果第二个模型仍处理同一种营养任务，可以继续共用当前 Topic/Tag。如果是新业务任务，再新增 Tag 和对应 Listener。

## 16. API 验收样例

### 16.1 提交

```http
POST /ai/tasks
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "clientRequestId": "ai-demo-20260909-001",
  "aiType": 1,
  "aiApplication": 1,
  "materials": {
    "chicken": "150g",
    "rice": "200g"
  },
  "cookWay": "steam",
  "flag": "DIET",
  "universalId": 10001
}
```

预期：

```http
HTTP/1.1 202 Accepted
Location: /ai/tasks/{taskId}
```

请求体中不应出现 `userId`。

### 16.2 查询状态

```http
GET /ai/tasks/{taskId}
Authorization: Bearer <access-token>
```

### 16.3 查询结果

```http
GET /ai/tasks/{taskId}/result
Authorization: Bearer <access-token>
```

- 处理中状态返回 `202`。
- `SUCCESS/FAILED/DEAD_LETTER` 返回 `200`，业务结果通过响应体 `status` 表达。
- 其他用户的 JWT 不能查询该任务。

## 17. 运维查询

查看任务：

```sql
SELECT id, user_id, client_request_id, task_type, status,
       attempt_count, max_attempts, worker_id, lease_until,
       error_code, error_message, create_time, queue_time,
       start_time, finish_time, update_time, version_id
FROM ai_task
WHERE id = ?;
```

查看 Outbox：

```sql
SELECT id, event_id, aggregate_id, event_type, topic, tag,
       status, retry_count, max_retries, next_retry_time,
       broker_message_id, last_error, create_time, update_time
FROM mq_outbox
WHERE aggregate_type = 'AI-TASK'
  AND aggregate_id = ?
ORDER BY id;
```

| 现象 | 检查重点 |
| --- | --- |
| `PENDING` 很久 | 初始 Outbox 状态、Publisher 日志、RocketMQ 连接 |
| `QUEUED` 很久 | Consumer Lag、DLQ、Consumer 是否启动 |
| `RUNNING` 很久 | `lease_until` 是否续期、模型请求是否卡死 |
| `RETRY_WAIT` 很久 | RocketMQ 重投、DLQ、`attempt_count` |
| `DEAD_LETTER` | 任务 Outbox 的 `last_error` |
| 终态但通知未到 | `AI_COMPLETED/AI_FAILED` Outbox 状态与下游消费者 |

## 18. 验证

编译：

```powershell
mvn -pl diary-AI -am -DskipTests compile
```

手工与联调用例见：

```text
diary-AI简化版测试用例.md
```

核心验收项：

- [ ] 请求体不再信任 `userId`。
- [ ] 相同幂等请求只创建一个任务。
- [ ] Redis 仅存在提交限流键。
- [ ] 状态查询始终返回 MySQL 最新状态。
- [ ] Outbox 成功、失败、超时恢复和 DEAD 都能收敛。
- [ ] 重复 MQ 消息不重复调用模型。
- [ ] 租约过期 RUNNING 可恢复。
- [ ] 等待态不再自动生成额外补发世代。
- [ ] 第二模型可通过唯一 code 注册到 `AIFactory`。

## 19. 与版本 3 的边界

出现以下条件时再启动多实例改造：

1. 单实例在稳定负载下成为经证明的瓶颈。
2. 需要不停机发布或实例级高可用。
3. 已有完整监控、告警、DLQ 处理和状态机回归能力。
4. 至少两个 AI 实例会同时运行。

到时再评估：

- 分布式调度锁。
- 全局模型限流/信号量。
- Outbox 批量领取和锁定者身份。
- 多实例缓存一致性（如果届时确实需要缓存）。
- 实例级指标与优雅停机。

版本 2 的原则是：先让单实例业务闭环简单、可读、可恢复，不为尚未发生的集群问题提前付出复杂度。
