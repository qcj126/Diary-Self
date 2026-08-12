# diary-AI 接入 RocketMQ 异步改造方案

## 1. 文档定位

本文档用于指导 Diary-Self 项目将 diary-AI 中的 Qwen Plus 营养分析链路改造成基于 RocketMQ 5.x 的异步任务。

本方案只提供设计和实施步骤，不包含实际代码改动。

### 1.1 本期唯一业务基线

本期仅以以下实现为准：

```text
diary.diaryai.strategy.nutrientanlalyze.InvokeQwenPlus
```

本期业务不是图片识别，而是对已有饮食或食谱的食材、用量、佐料和烹饪方式进行营养分析。

`InvokeQwenPlus` 当前负责：

1. 接收食材列表 `materials`。
2. 根据食材、用量和烹饪方式构造 Prompt。
3. 调用 DashScope Qwen Plus 模型。
4. 从模型响应中解析营养分析 JSON。
5. 保存 AI 调用记录和营养分析结果。
6. 通过 `flag + universalId` 将结果关联到饮食或食谱业务对象。

本链路不需要 OSS object key、图片签名 URL 或 Redis 图片映射。

### 1.2 本期明确不作为设计依据的内容

- 其他大模型策略的现有实现。
- `diary-common` 中当前旧版 AI SQL 文件。
- 前端 Token 尚未完成的真实用户身份传递。
- 将所有 AI 模型一次性迁移到 RocketMQ。
- 本期直接拆分独立的 AI Worker 微服务。

### 1.3 userId 约定

本期继续使用：

```java
userId = 10000L
```

消息协议中仍建议保留 `userId` 字段，并固定传入 `10000L`。这样前端 Token 完善后只需替换用户 ID 来源，不需要重新修改 Topic、消息协议和消费者主流程。

## 2. 改造目标

当前接口调用是同步流程：

```text
POST /ai/invoke
    ↓
参数校验
    ↓
选择 InvokeQwenPlus
    ↓
调用 Qwen Plus
    ↓
解析模型结果
    ↓
写入数据库
    ↓
返回“处理成功”
```

改造后的目标流程：

```text
客户端提交营养分析请求
        ↓
diary-AI 校验请求并生成 taskId
        ↓
保存任务信息和稳定的输入快照
        ↓
向 RocketMQ 投递任务消息
        ↓
接口立即返回 taskId 和当前提交状态
        ↓
diary-AI RocketMQ Consumer 消费任务
        ↓
读取任务输入并调用 InvokeQwenPlus
        ↓
保存 AI 结果并更新任务状态
        ↓
发布 AI_COMPLETED 或 AI_FAILED 事件
        ↓
diary-notify 消费事件并推送用户
```

核心目标包括：

- HTTP 请求不再等待大模型执行完成。
- AI 调用失败后能够有限重试。
- 重复投递不会产生重复营养结果。
- 服务重启后仍然能够恢复未完成任务。
- 可以查询任务的排队、执行、成功和失败状态。
- 能够观察消息积压、消费耗时和模型异常。
- 为后续其他 AI 模型和其他模块提供可复用范式。

## 3. 推荐的第一版范围

第一版建议只学习和实现以下能力：

- RocketMQ 5.x gRPC 客户端连接。
- Normal Topic。
- Producer 同步发送。
- PushConsumer 或 SimpleConsumer 消费。
- Topic、Tag、Message Key、Consumer Group。
- 任务状态机。
- 消费幂等。
- 有限消费重试。
- 死信队列。
- AI 完成和失败事件。
- Dashboard 中的消息查询与消费积压观察。

第一版暂时不要加入：

- FIFO 消息。
- 延时消息。
- 事务消息。
- 多种 AI 模型共用同一个复杂消费者。
- 自动弹性扩容。
- 跨服务分布式事务。

先把 Normal 消息链路做正确，再把 Outbox、事务消息等作为第二阶段练习。

## 4. 推荐架构

### 4.1 逻辑组件

```text
DiaryAIController
        ↓
AiTaskApplicationService
        ├── 创建任务/保存输入快照
        └── 调用 AiTaskProducer
                    ↓
             diary-ai-task
                    ↓
             AiTaskConsumer
                    ↓
          QwenPlusTaskExecutor
                    ↓
              InvokeQwenPlus
                    ↓
       任务状态 + AI结果持久化
                    ↓
            AiEventProducer
                    ↓
            diary-ai-event
                    ↓
              diary-notify
```

### 4.2 类职责建议

| 建议组件 | 职责 |
| --- | --- |
| `AiTaskController` | 提交任务、查询任务、查询结果 |
| `AiTaskApplicationService` | 参数校验、幂等提交、创建任务、发送任务消息 |
| `AiTaskProducer` | 构建并发送 RocketMQ 消息，不包含 AI 业务逻辑 |
| `AiTaskConsumer` | 解析消息、幂等判断、抢占任务、控制消费结果 |
| `QwenPlusTaskExecutor` | 编排 Qwen Plus 的构建 Prompt、调用、解析和落库 |
| `InvokeQwenPlus` | 保留具体模型能力，不直接关注 HTTP 和 RocketMQ |
| `AiTaskRepository/Mapper` | 任务状态和输入快照持久化 |
| `AiEventProducer` | 发布成功或失败领域事件 |
| `AiTaskRecoveryJob` | 恢复长时间卡在 RUNNING 的任务 |

不建议让 RocketMQ Listener 中堆积全部业务代码。Listener 应只负责消费协议、状态控制和调用任务执行器。

## 5. API 设计

### 5.1 提交任务

可以保留旧接口路径以减少前端改动，也可以使用更清晰的新路径：

```text
POST /ai/tasks
```

请求示例：

```json
{
  "clientRequestId": "web-20260812-000001",
  "aiType": 3,
  "aiApplication": 1,
  "flag": "DIET",
  "universalId": 1234567890,
  "materials": [
    {
      "pork": "300g",
      "vegetables": "200g",
      "cookWay": "爆炒"
    }
  ]
}
```

`clientRequestId` 建议由前端为一次用户操作生成，用于防止用户重复点击导致重复创建任务。

接口立即返回 HTTP 202：

```json
{
  "taskId": "2000000000001",
  "status": "QUEUED",
  "message": "AI分析任务正在处理中"
}
```

当第一版使用同步发送且 Broker 已确认消息时，返回 `QUEUED`。只有任务已入库但消息尚未发送或等待补偿时才是 `PENDING`。

不要再返回“AI 调用成功，数据已经处理”，因为此时只代表任务提交成功。

需要明确区分：

```text
任务创建成功 ≠ 消息发送成功
消息发送成功 ≠ AI执行成功
AI执行成功 ≠ 用户通知成功
```

### 5.2 查询任务状态

```text
GET /ai/tasks/{taskId}
```

响应建议包含：

```json
{
  "taskId": "2000000000001",
  "userId": 10000,
  "status": "RUNNING",
  "attemptCount": 1,
  "model": "qwen3.7-plus",
  "createdAt": "2026-08-12T10:00:00+08:00",
  "startedAt": "2026-08-12T10:00:02+08:00",
  "finishedAt": null,
  "resultId": null,
  "errorCode": null,
  "errorMessage": null
}
```

### 5.3 查询任务结果

```text
GET /ai/tasks/{taskId}/result
```

- `SUCCESS`：返回营养分析结果。
- `PENDING/QUEUED/RUNNING`：返回任务尚未完成。
- `FAILED`：返回适合前端展示的失败原因。

### 5.4 可选的人工重试

```text
POST /ai/tasks/{taskId}/retry
```

只允许对 `FAILED` 或 `DEAD_LETTER` 状态进行人工重试，并生成新的任务消息 eventId。不要直接修改 Broker 中的旧消息。

## 6. 输入模型设计

### 6.1 营养分析输入语义

当前 `AiInvokeDTO` 的字段含义为：

```text
NutrientAnalyzeTaskInput
├── clientRequestId  客户端提交幂等键
├── userId = 10000  当前固定用户
├── aiType          模型策略，本期应限定为 QWENPLUS
├── aiApplication   AI 用途，例如营养分析
├── flag            DIET 或 RECIPE
├── universalId     饮食 ID 或食谱 ID
└── materials       食材、佐料、用量、烹饪方式
```

`flag` 决定 `universalId` 的业务类型，两者必须同时存在并且匹配。建议第一版只允许一个任务分析一个 `universalId`，不在单条消息中批量处理多个饮食或食谱。

### 6.2 使用稳定输入快照

任务创建时将已校验的 `clientRequestId`、`aiType`、`aiApplication`、`flag`、`universalId` 和 `materials` 保存到 `input_snapshot`。消费者不应再从前端或其他服务还原当时的食材数据。

快照建议在入库前做归一化：

```text
校验 flag / universalId
校验 materials 非空、数量和字符长度
统一食材名称、数量和单位的表达
保存稳定 JSON 快照
```

`input_snapshot` 是任务重试和恢复的依据。用户之后修改食谱或饮食内容时，已创建任务仍按提交时的快照分析。

### 6.3 `materials` 结构改进

当前 `List<Map<String,String>>` 可以作为过渡实现，但字段名和单位不受编译期约束。后续建议改为有类型的食材 DTO，至少包含 `name`、`amount`、`unit`，烹饪方式作为任务级字段或明确的 `cookWay`。

不建议用动态食材名作为 JSON key，否则难以统一校验、单位换算和 Prompt 生成。

### 6.4 模型输出契约

一个任务对应一个 `flag + universalId`，因此模型应返回该饮食或食谱的整体营养估算，而不是每个食材分别返回一条数据。推荐统一为单对象：

```json
{
  "calory": 680,
  "protein": 42.5,
  "fat": 31.2,
  "carbohydrate": 55.8,
  "sugar": 6.1,
  "sodium": 920
}
```

建议使用稳定英文字段名和统一单位，数据库保存数值，单位由协议规定，不要将 `"xx kcal"`、`"xx g"` 直接作为不可计算的字符串。如果暂时保留现有中文字段和带单位字符串，需在落库前做严格校验。

## 7. 任务数据设计

虽然旧 AI SQL 不纳入本期依据，但异步任务仍然需要新的、与当前业务匹配的持久化状态。表结构由你根据真实数据库重新设计，本文只定义必要语义。

### 7.1 任务记录必要字段

| 字段 | 说明 |
| --- | --- |
| `task_id` | AI 任务唯一 ID |
| `user_id` | 本期固定为 10000 |
| `client_request_id` | 提交幂等键 |
| `task_type` | `QWEN_PLUS_NUTRIENT` |
| `ai_type` | AI 类型 |
| `ai_application` | AI 应用场景 |
| `flag` | 饮食或食谱等业务标识 |
| `universal_id` | `flag` 对应的饮食 ID 或食谱 ID |
| `model` | 实际模型，例如 `qwen3.7-plus` |
| `status` | 任务状态 |
| `input_snapshot` | 稳定的任务输入 JSON，或输入表引用 |
| `attempt_count` | 已执行次数 |
| `max_attempts` | 任务允许的最大执行次数 |
| `worker_id` | 当前处理任务的实例 |
| `lease_until` | RUNNING 任务租约截止时间 |
| `ai_info_id` | 成功后关联现有 AI 信息记录 |
| `error_code` | 稳定错误码 |
| `error_message` | 截断后的错误摘要 |
| `created_at` | 创建时间 |
| `queued_at` | 消息成功发送时间 |
| `started_at` | 开始执行时间 |
| `finished_at` | 执行结束时间 |
| `version` | 乐观锁版本 |

### 7.2 唯一约束建议

```text
UNIQUE(user_id, client_request_id)
```

它可以防止用户双击或网络重试造成重复任务。

结果表还应建立能阻止同一任务重复落库的唯一约束，例如：

```text
UNIQUE(task_id)
```

建议在 `ai_nutrient` 中增加 `task_id`。`flag + universal_id` 可以用于查询当前业务对象的营养结果，但不建议直接做永久唯一键，否则无法保留重新分析的历史版本。

`AiNutrientPO` 落库时必须同时写入 `flag` 和 `universalId`。不能只写 `universalId`，因为饮食 ID 和食谱 ID 可能取值相同。

## 8. 状态机设计

第一版推荐状态：

```text
PENDING
   │ 创建任务
   ▼
QUEUED
   │ Consumer 成功抢占
   ▼
RUNNING
   ├──→ SUCCESS
   ├──→ RETRY_WAIT ───→ RUNNING
   └──→ FAILED
```

第二阶段再增加：

```text
CANCEL_REQUESTED
CANCELLED
DEAD_LETTER
```

### 8.1 状态含义

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 数据库已创建任务，消息尚未确认发送成功 |
| `QUEUED` | 任务消息已发送到 RocketMQ |
| `RUNNING` | 某个消费者正在执行 Qwen Plus 调用 |
| `RETRY_WAIT` | 本次执行发生可重试错误，等待再次投递 |
| `SUCCESS` | 模型结果已经可靠保存 |
| `FAILED` | 永久错误或达到最大任务尝试次数 |
| `DEAD_LETTER` | 消息进入死信，需要人工检查 |

### 8.2 状态只能按条件更新

消费者抢占任务必须执行类似的条件更新：

```text
只有 QUEUED / RETRY_WAIT / 租约已过期的 RUNNING
才允许更新为 RUNNING
```

成功状态属于终态：

```text
如果任务已经 SUCCESS，重复消息直接返回消费成功，不能再次调用模型。
```

### 8.3 RUNNING 租约

Consumer 调用 Qwen Plus 时可能宕机。任务需要记录：

```text
workerId
leaseUntil
```

XXL-Job 或恢复任务定期扫描：

```text
status = RUNNING AND lease_until < now()
```

将它们恢复为 `RETRY_WAIT` 并重新发布任务消息。必须同时检查 `attempt_count`，超过最大次数后进入 `FAILED`。

## 9. RocketMQ 资源规划

### 9.1 任务 Topic

```text
Topic: diary-ai-task
MessageType: NORMAL
Tag: QWEN_PLUS_NUTRIENT
ConsumerGroup: diary-ai-qwen-plus-worker-v1
```

用途：驱动 Qwen Plus 营养分析任务。

### 9.2 结果事件 Topic

```text
Topic: diary-ai-event
MessageType: NORMAL
Tags:
  AI_COMPLETED
  AI_FAILED
ConsumerGroup:
  diary-notify-ai-event-v1
```

用途：将 AI 最终结果通知 diary-notify 或其他下游消费者。

### 9.3 环境隔离

至少保证开发、测试和生产不能互相消费。可以使用不同 RocketMQ 集群、Namespace，或者在 Topic 名中体现环境：

```text
diary-dev-ai-task
diary-test-ai-task
diary-prod-ai-task
```

练习环境中推荐显式加环境前缀，便于在 Dashboard 中辨认。

### 9.4 为什么第一版不用 FIFO

每个营养分析任务相互独立，不要求全局顺序，也不要求同一用户的任务严格串行。因此 Normal Topic 更合适。

只有以后出现“同一业务对象的多阶段事件必须严格按顺序执行”时，再考虑 FIFO 和 messageGroup。

### 9.5 为什么第一版不用 Delay

AI 失败重试应先使用 RocketMQ 的消费重试策略。不要收到错误后手动不停发送延时消息，否则容易出现两套重试机制叠加。

Delay Topic 可在后续用于人工补偿或特定业务延期，不是本期主链路必需能力。

## 10. 消息协议

### 10.1 AI 任务消息

建议消息体只传任务索引信息：

```json
{
  "eventId": "evt-2000000000001",
  "taskId": "2000000000001",
  "userId": 10000,
  "taskType": "QWEN_PLUS_NUTRIENT",
  "schemaVersion": 1,
  "occurredAt": 1786490400000,
  "traceId": "trace-xxxx"
}
```

建议 RocketMQ 属性：

```text
Topic = diary-ai-task
Tag = QWEN_PLUS_NUTRIENT
Key = taskId
```

食材列表、完整 Prompt 和 AI 响应不需要塞入 RocketMQ 消息。消费者通过 `taskId` 读取持久化输入快照。

### 10.2 AI 成功事件

```json
{
  "eventId": "evt-completed-2000000000001",
  "eventType": "AI_COMPLETED",
  "taskId": "2000000000001",
  "userId": 10000,
  "aiInfoId": "3000000000001",
  "schemaVersion": 1,
  "occurredAt": 1786490500000,
  "traceId": "trace-xxxx"
}
```

### 10.3 AI 失败事件

```json
{
  "eventId": "evt-failed-2000000000001",
  "eventType": "AI_FAILED",
  "taskId": "2000000000001",
  "userId": 10000,
  "errorCode": "MODEL_RESPONSE_INVALID",
  "retryable": false,
  "schemaVersion": 1,
  "occurredAt": 1786490500000,
  "traceId": "trace-xxxx"
}
```

### 10.4 协议原则

- `eventId` 每次事件唯一。
- `taskId` 在整个任务生命周期内不变。
- Message Key 使用 taskId，便于 Dashboard 查询。
- `schemaVersion` 从 1 开始。
- 消息只包含必要索引，不发送大段食材数据、Prompt 或模型结果。
- 消息反序列化失败不能无限重试。
- 未知 `schemaVersion` 要记录明确错误并进入人工处理流程。

## 11. Producer 设计

### 11.1 第一阶段：直接发送

练习 RocketMQ 基础时可以先实现：

```text
创建任务记录
    ↓
发送 Normal Message
    ↓
发送成功：任务改为 QUEUED
发送失败：任务保留 PENDING，等待补偿
```

Producer 必须记录：

- eventId
- taskId
- Topic
- Tag
- Message Key
- RocketMQ messageId
- 发送结果
- 发送耗时
- 失败异常

不要认为客户端自带重试一定能成功。最终发送失败后必须留下可恢复的数据库状态。

### 11.2 第二阶段：Outbox

完成基础链路后，推荐增加 Outbox：

```text
同一个数据库事务：
  插入 AI任务
  插入任务输入
  插入 mq_outbox
提交
```

Outbox Publisher 再负责发送 RocketMQ：

```text
NEW → SENDING → SENT
              ↘ FAILED → 按 nextRetryAt 重试
```

Outbox 发布器多实例运行时，要使用乐观锁或数据库跳过锁定行等方式抢占消息。

即使使用 Outbox，仍可能发生：

```text
Broker 已收到消息
但应用更新 Outbox 为 SENT 前宕机
```

因此消息可能重复发送，消费者幂等仍然必不可少。

### 11.3 第三阶段：事务消息练习

等 Outbox 方案完全掌握后，再建立独立的 Transaction Topic，练习：

```text
发送 Half Message
    ↓
执行创建 AI任务的本地事务
    ↓
Commit / Rollback
    ↓
Broker 必要时回查 taskId 的数据库状态
```

事务回查必须查询数据库，不得查询内存变量。

事务消息解决生产端本地事务与消息提交的最终一致性，不替代消费者幂等，也不保证下游立即成功。

## 12. Consumer 设计

### 12.1 标准消费步骤

```text
1. 解析消息并校验 schemaVersion
2. 根据 taskId 查询任务
3. 检查任务是否已经到达终态
4. 原子抢占任务并更新 RUNNING、workerId、leaseUntil
5. 读取任务输入快照
6. 校验 flag、universalId 和 materials
7. 调用 InvokeQwenPlus 构造 Prompt
8. 调用 Qwen Plus
9. 校验并解析模型响应
10. 生成当前饮食或食谱的单条汇总营养结果
11. 在本地事务中保存 AiInfoPO、AiNutrientPO 并更新任务 SUCCESS
12. 创建 AI_COMPLETED Outbox 事件
13. 返回消费成功
```

### 12.2 不要在 Listener 内再使用 @Async

错误做法：

```text
Listener 收到消息
    ↓
扔给 @Async 线程
    ↓
Listener 立即返回成功
```

这样 RocketMQ 会认为消息已消费完成。如果异步线程随后失败，Broker 不会按照正常消费失败流程重投。

Listener 应等待本次业务处理完成后再返回消费结果。

### 12.3 PushConsumer 与 SimpleConsumer

Qwen Plus 调用属于长耗时外部请求，选择消费者类型时要考虑实际耗时分布。

推荐顺序：

1. 如果大多数调用能在可预测的几十秒内完成，先使用 PushConsumer 学习完整链路。
2. 如果调用经常达到数分钟、耗时波动大，使用 SimpleConsumer，并设置略长于正常执行时间的 invisible duration。
3. 必须给 DashScope SDK 配置连接、读取和总执行超时。
4. 消费超时可能造成重复投递，因此任务状态机和租约必须能够处理重复消息。

不要为了避免消息超时而设置无限长超时。超时太长也会让宕机任务很久不能恢复。

## 13. 幂等设计

RocketMQ 是至少一次投递。重复消息不是异常情况，而是必须支持的正常语义。

### 13.1 第一层：任务状态幂等

```text
任务不存在：按错误分类处理
任务 SUCCESS：直接返回消费成功
任务 FAILED/CANCELLED：直接返回消费成功
任务 RUNNING 且租约有效：不再次执行
任务 RUNNING 且租约过期：允许恢复
任务 QUEUED/RETRY_WAIT：尝试原子抢占
```

### 13.2 第二层：消费事件幂等

可建立消费记录，唯一键：

```text
consumer_group + event_id
```

不要在 AI 业务完成前就将消费记录标记为成功。否则处理中途失败后，下一次重投会被错误拦截。

理想事务边界：

```text
保存 AiInfoPO
保存 AiNutrientPO
更新任务 SUCCESS
写入消费完成记录
写入 AI_COMPLETED Outbox
```

以上操作尽量在同一个本地数据库事务中完成。

### 13.3 结果幂等

为任务结果建立数据库唯一约束，避免同一任务重复插入：

```text
taskId
```

如果现有表暂时没有 `taskId`，可通过 `aiInfoId` 与任务建立稳定的一对一关系，但仍需有数据库层唯一约束。同一 `flag + universalId` 允许用户主动发起新任务，新旧分析用不同 `taskId` 区分。

### 13.4 无法完全消除的重复模型调用窗口

可能发生：

```text
Qwen Plus 已返回成功
    ↓
数据库尚未提交
    ↓
Consumer 宕机
    ↓
消息重投后再次调用 Qwen Plus
```

缓解措施：

- 将 taskId 作为第三方请求幂等标识（如果 API 支持）。
- 保存供应商 requestId。
- 记录每次模型调用流水。
- 设置每个任务最大模型调用次数。
- 将模型原始响应先可靠保存，再解析和生成营养结果。
- 统计重复调用造成的 Token 和费用。

MQ 无法自动消除所有第三方调用与本地数据库之间的不一致窗口。

## 14. 重试分类

### 14.1 可重试错误

- 网络连接超时。
- DashScope 429 限流。
- DashScope 5xx。
- 数据库临时不可用。
- RocketMQ 临时连接异常。
- 模型偶发返回空内容。

### 14.2 不可重试错误

- `materials` 为空、超出数量限制或缺少必要的食材信息。
- `flag` 不支持，或 `flag + universalId` 无法对应饮食/食谱对象。
- 食材数量、单位或烹饪方式格式无法校验。
- `aiType` 与 Qwen Plus 任务不匹配。
- API Key 无效或无模型访问权限。
- 消息 schemaVersion 不支持。
- 任务已取消或已经成功。
- 模型连续返回无法解析的固定错误格式，并达到业务修复次数。

### 14.3 建议重试层级

必须避免多层重试相乘：

```text
SDK内部重试 × 业务代码重试 × RocketMQ消费重试
```

第一版建议：

- DashScope SDK 网络重试：0～1 次。
- JSON 格式修复调用：最多 1 次。
- RocketMQ 消费重试：3～5 次。
- 任务最大实际模型调用次数：单独限制，例如 3 次。

`attemptCount` 应表示真实进入模型执行流程的次数，而不是只统计 RocketMQ delivery 次数。

### 14.4 响应解析失败

当前 `extractResult` 直接将模型文本反序列化为 `List<Map<String,String>>`。对一个 `universalId` 的整体营养分析，建议协议明确返回一个结果对象；如果暂时保留数组，则必须校验数组长度为 1。还应增加：

- choices 是否存在。
- content 是否存在。
- text 是否存在。
- 是否为纯 JSON 数组。
- 结果数量是否符合“一个业务对象一条汇总结果”的约定。
- 所有必要营养字段是否存在。
- 数值和单位是否符合预期。
- 数值是否为非负数且不超出合理上限。

如果只是 Markdown 包裹或轻微 JSON 格式问题，可以本地清洗一次；不要每次解析异常都无限重新调用模型。

## 15. 死信处理

消息达到 Consumer Group 最大重试次数后会进入死信队列。不能只依赖 Broker 保存死信而不处理。

需要建立：

- DLQ 数量告警。
- 根据 taskId 查询任务详情的能力。
- 人工判断是否能够重投。
- 重投前修复输入、配置或模型权限的流程。
- 重投审计记录。

推荐做法：

```text
消息进入 DLQ
    ↓
告警或补偿任务发现
    ↓
任务状态更新为 DEAD_LETTER
    ↓
人工检查 errorCode / attemptCount / 输入快照
    ↓
决定重新创建消息或终止任务
```

人工重投应该生成新的 eventId，同时沿用原 taskId 并记录操作人和原因。

## 16. 并发、限流和费用保护

RocketMQ 的消费速度可能远高于 Qwen Plus 的接口配额。消费者并发不能直接使用默认高线程数。

### 16.1 并发估算

粗略计算：

```text
最大并发 ≈ 允许QPS × 平均调用耗时秒数
```

练习阶段建议从 1～2 个消费者线程开始，观察：

- Qwen Plus 平均耗时。
- 429 比例。
- Consumer Lag。
- JVM 线程与内存。
- 单任务 Token 和费用。

确认稳定后再逐步增加。

### 16.2 需要设置的保护

- Consumer 线程数。
- 单实例 Qwen Plus 最大并发。
- 全集群最大并发。
- 每个任务最大食材数量。
- 单个食材名称、用量、单位和烹饪方式的长度限制。
- 单任务最大输入长度。
- 单任务最大模型调用次数。
- 单位时间任务提交次数。
- 每日 Token 或费用预算。

### 16.3 不要用失败重试实现限流

如果达到 Qwen Plus 配额，不要让所有 Consumer 通过抛异常进入高频重试。应降低消费并发、使用本地或分布式限流，并让消息在 Broker 中形成可观察的正常积压。

## 17. AI 完成事件接入 diary-notify

Qwen Plus 任务执行成功后发布 `AI_COMPLETED`，失败后发布 `AI_FAILED`。

diary-notify 将其转换成前端协议：

```text
AI_COMPLETED → AI_COMPLETE
AI_FAILED    → AI_FAILED
```

通知内容建议只包含：

```text
taskId
aiInfoId/resultId
userId = 10000
前端跳转所需的最小信息
```

不要在通知消息中发送完整营养分析结果，前端收到通知后通过查询接口获取结果。

通知消费也需要幂等，否则 AI 完成事件重投时会生成重复离线消息。

本期可以分两步：

1. 先完成 `diary-ai-task` 主链路和任务查询。
2. 主链路稳定后再接 `diary-ai-event → diary-notify`。

## 18. 配置规划

当前使用的是：

```text
rocketmq-v5-client-spring-boot-starter
```

这是 RocketMQ 5.x gRPC 客户端。配置中的 endpoint 必须指向启用了 gRPC 的 RocketMQ Proxy，不能误填 Dashboard 端口或传统 NameServer 端口。

建议由 Nacos 管理：

```text
rocketmq endpoints
accessKey / secretKey
taskTopic
eventTopic
taskConsumerGroup
notifyConsumerGroup
producer request timeout
consumer类型
consumer线程数
消费最大重试次数
任务最大执行次数
DashScope调用超时
RUNNING租约时长
Outbox扫描间隔
环境标识
```

注意：

- `rocketmq-v5-client-spring-boot-starter` 不等同于旧版 `rocketmq-spring-boot-starter`。
- 不要复制旧客户端的 `name-server`、`RocketMQTemplate` 或 Listener 注解示例，除非确认当前 2.3.6 版本确实支持相同 API。
- API Key、AccessKey 和 SecretKey 不应提交到 Git。
- 当前 `192.168.101.128:8081` 必须确认是 Proxy gRPC endpoint。

## 19. 可观测性

### 19.1 日志字段

所有 Producer、Consumer 和 Qwen Plus 调用日志统一携带：

```text
traceId
eventId
taskId
userId
topic
tag
rocketmqMessageId
deliveryAttempt
taskAttemptCount
model
providerRequestId
durationMs
```

不要在日志中完整打印 API Key、用户食谱/饮食内容、超长 Prompt 和完整模型响应。

### 19.2 RocketMQ 指标

- 发送成功率和失败率。
- Consumer Group 在线实例数。
- 消息积压量。
- 最老积压消息时间。
- 消费成功率。
- 重试消息数量。
- DLQ 数量。
- 消费耗时 P95/P99。

### 19.3 AI 业务指标

- PENDING、QUEUED、RUNNING、SUCCESS、FAILED 数量。
- RUNNING 租约过期数量。
- Qwen Plus 成功率。
- Qwen Plus 429、超时、5xx 数量。
- 模型响应解析失败率。
- 单任务平均食材数和输入长度。
- 单任务平均耗时。
- 每日模型调用次数。
- 重复模型调用次数。
- Token 和费用。

## 20. 分阶段实施顺序

### 阶段 0：整理 Qwen Plus 输入输出边界

- 明确 `InvokeQwenPlus` 接收的食材、用量、单位和烹饪方式数据结构。
- 用类型明确的 DTO 替代核心流程中的裸 Map。
- 明确 `flag + universalId` 与饮食/食谱业务对象的关系。
- 明确一个任务产生一条对应业务对象的汇总营养结果。
- 增加 Qwen Plus 返回结果的完整校验。

完成标准：不接 RocketMQ 时，Qwen Plus 单独调用也具有稳定输入输出。

当前代码的 Prompt 还在要求返回“多组数据数组 + uniqueId”，应在此阶段先改为“单个饮食/食谱的汇总营养对象”，再接入重试和幂等链路。

### 阶段 1：RocketMQ 最小连通实验

```text
测试接口
  → diary-ai-task
  → 测试 Consumer
  → 打印 taskId
```

学习内容：

- Proxy endpoint。
- Normal Topic。
- Producer。
- Consumer Group。
- Tag 和 Message Key。
- Dashboard 消息查询。

完成标准：消息能稳定发送和消费，Dashboard 能根据 Key 查到消息。

### 阶段 2：建立 AI 任务状态

- 创建任务记录和输入快照。
- 提交接口立即返回 taskId。
- 增加任务查询接口。
- Producer 发送 taskId 消息。
- 发送成功更新 QUEUED，失败保留 PENDING。

完成标准：暂时不调用 Qwen Plus，也能完整观察 PENDING → QUEUED → RUNNING → SUCCESS 的模拟状态。

### 阶段 3：接入 InvokeQwenPlus

- Consumer 加载任务输入。
- 验证 `flag + universalId` 并加载食材快照。
- 调用 `InvokeQwenPlus`。
- 保存 AiInfoPO 和 AiNutrientPO。
- 更新任务 SUCCESS。

完成标准：HTTP 请求立即返回，后台完成真实营养分析，查询接口能看到最终结果。

### 阶段 4：幂等与重复投递

- 重复发送同一个 eventId。
- 重复发送不同 eventId 但相同 taskId。
- Consumer 保存结果后、返回成功前模拟宕机。
- 验证 SUCCESS 任务不会再次调用 Qwen Plus。
- 验证营养结果不会重复插入。

完成标准：重复消息不会产生重复业务结果。

### 阶段 5：重试与 DLQ

主动制造：

- Qwen Plus 超时。
- 429。
- 5xx。
- 非法 JSON。
- `materials` 缺少必要字段或单位不合法。
- `flag + universalId` 不匹配。
- 数据库异常。
- 不存在的 taskId。

验证：

- 可重试异常进入有限重试。
- 永久异常直接 FAILED。
- 达到上限后进入 DLQ 并产生告警。
- attemptCount 与真实模型调用次数一致。

### 阶段 6：Outbox

- 任务创建和 Outbox 同事务提交。
- Broker 不可用时 Outbox 保留。
- 发布器恢复后自动发送。
- 模拟发送成功但 SENT 状态更新失败。
- 验证重复发送仍保持消费幂等。

### 阶段 7：AI 结果事件

```text
AI任务成功/失败
    → diary-ai-event
    → diary-notify
    → WebSocket或离线通知
```

完成标准：通知重复消费不产生重复离线消息。

### 阶段 8：限流和压测

- 批量提交任务。
- 从 1 个消费线程逐步增加。
- 观察积压、耗时和 429。
- 找到当前环境安全并发值。
- 验证优雅停机和 RUNNING 任务恢复。

### 阶段 9：事务消息对照实验

单独建立 Transaction Topic，对比：

- Outbox 与事务消息实现复杂度。
- 故障恢复方式。
- 事务回查机制。
- 数据库和 Broker 压力。
- 排障难度。

这一步主要用于学习，不建议在未完成前八个阶段时提前加入主链路。

## 21. 测试清单

| 测试场景 | 预期结果 |
| --- | --- |
| 正常提交饮食营养分析 | 立即返回 taskId，后台为该 DIET 落库一条汇总结果 |
| 正常提交食谱营养分析 | 立即返回 taskId，后台为该 RECIPE 落库一条汇总结果 |
| 多个食材和烹饪方式 | 作为同一业务对象整体计算，不按食材重复落库 |
| 重复 clientRequestId | 返回原任务或拒绝重复创建 |
| 同一 eventId 投递两次 | Qwen Plus 最多按幂等规则执行一次 |
| 同 taskId、不同 eventId | SUCCESS 后不再次执行 |
| Consumer 在保存成功后宕机 | 重投后识别 SUCCESS，不重复落库 |
| Consumer 在模型返回后、保存前宕机 | 允许恢复，重复模型调用被记录和限制 |
| Broker 不可用 | 任务保持 PENDING/Outbox NEW，恢复后补发 |
| Qwen Plus 超时 | 按有限策略重试 |
| Qwen Plus 429 | 降低消费压力并有限重试 |
| Qwen Plus 5xx | 有限重试 |
| API Key 无效 | 直接失败，不高频重试 |
| 返回非法 JSON | 本地修复或最多一次修复请求，之后失败 |
| `materials` 为空或超限 | 校验失败，不调用模型 |
| `flag` 非 DIET/RECIPE | 按永久错误处理 |
| `universalId` 缺失或与 flag 不匹配 | 不保存营养结果 |
| 模型返回多条汇总结果 | 拒绝落库并进入响应格式错误处理 |
| RUNNING 时 Worker 宕机 | 租约过期后可恢复 |
| 达到最大消费重试 | 进入 DLQ并更新任务状态 |
| 人工重投 DLQ | 新 eventId、原 taskId，留下审计记录 |
| 新 Consumer Group 首次上线 | 明确初始消费位置，避免漏掉已发送测试消息 |
| Consumer 多实例 | 同一任务不会并发重复执行 |
| 大量消息积压 | Broker 稳定，消费者按限流速度处理 |
| 服务优雅停机 | 不再接收新任务，当前任务完成或可恢复 |
| diary-notify 暂时不可用 | AI任务仍成功，完成事件可稍后消费 |

## 22. 常见错误清单

- 只把同步方法放进 Listener，没有任务状态表。
- HTTP 返回“AI 成功”，实际上只是消息发送成功。
- 消息中塞入完整食材快照、Prompt 或完整模型结果。
- 任务执行时重新查询已被用户修改的食谱/饮食内容，而不使用提交时快照。
- 未校验 `flag + universalId` 就将营养结果关联到业务数据。
- Consumer 收到消息后再提交给 `@Async` 并立即返回成功。
- 使用默认高并发直接压 Qwen Plus。
- 同时开启 SDK、业务和 MQ 的高次数重试。
- 捕获异常后打印日志并返回消费成功。
- 重复投递时再次写入营养结果。
- 将 Topic、Tag 和 Consumer Group 写死在多个类中。
- 把 Normal、FIFO、Delay、Transaction 消息放入同一个 Topic。
- 混用 RocketMQ 4.x 和 5.x 客户端配置示例。
- 把 Proxy、NameServer 和 Dashboard 端口混淆。
- 没有监控消息积压和 DLQ。
- 直接在 AI Consumer 中调用 WebSocket 推送。

## 23. 本期验收标准

满足以下条件，可认为 Qwen Plus 异步改造第一版完成：

1. 提交接口在短时间内返回 taskId，不等待 Qwen Plus。
2. 任务状态能够正确经历 PENDING、QUEUED、RUNNING 和终态。
3. Qwen Plus 营养分析能够由 RocketMQ Consumer 执行。
4. 食材、用量、烹饪方式以及 `flag + universalId` 可以从持久化输入快照恢复。
5. 重复消息不会重复调用或重复保存最终结果。
6. 可重试错误具有有限重试，永久错误不会无限重试。
7. 达到重试上限的消息可在 DLQ 中查询并告警。
8. Broker 短暂不可用不会永久丢失已创建任务。
9. 能在 Dashboard 中按 taskId 定位消息。
10. 日志能够串联 HTTP 请求、任务、RocketMQ 消息和 Qwen Plus 调用。
11. Consumer 并发受控，不会轻易触发大量 429。
12. userId 虽固定为 10000，但消息和任务模型已保留用户字段。
13. 成功和失败事件可以独立发布，为 diary-notify 接入做好准备。

## 24. 推荐学习重点

完成这次改造时，重点理解以下问题：

1. 为什么异步任务必须有独立状态，而不能只依赖 MQ 中是否有消息。
2. 为什么 RocketMQ 发送成功不等于 AI 业务成功。
3. 为什么至少一次投递必然要求消费者幂等。
4. 为什么 Consumer 不能收到消息后随意开启后台线程并提前 ACK。
5. 为什么第三方 AI 调用无法天然与本地数据库形成原子事务。
6. Outbox 和 RocketMQ 事务消息分别解决什么问题。
7. PushConsumer 和 SimpleConsumer 如何适配不同执行时长。
8. 如何区分可重试错误与永久错误。
9. 如何避免多层重试导致模型调用次数和费用指数放大。
10. 如何通过积压而不是失败重试实现削峰。
11. Topic、Tag、Key 和 Consumer Group 分别承担什么职责。
12. 如何通过状态机、日志、指标和 DLQ 恢复故障任务。

## 25. 官方资料

- [RocketMQ 5.x 核心概念](https://rocketmq.apache.org/docs/introduction/02concepts/)
- [RocketMQ Java 5.x gRPC SDK](https://rocketmq.apache.org/docs/sdk/02java/)
- [RocketMQ 消息过滤与 Tag](https://rocketmq.apache.org/docs/featureBehavior/07messagefilter/)
- [RocketMQ 消费者类型](https://rocketmq.apache.org/docs/featureBehavior/06consumertype/)
- [RocketMQ 消费重试与死信](https://rocketmq.apache.org/docs/featureBehavior/10consumerretrypolicy/)
- [RocketMQ 事务消息](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/)
- [RocketMQ 消费进度与 Offset](https://rocketmq.apache.org/docs/featureBehavior/09consumerprogress/)
- [RocketMQ 参数限制](https://rocketmq.apache.org/docs/introduction/03limits/)
- [RocketMQ Dashboard](https://rocketmq.apache.org/docs/deploymentOperations/04Dashboard/)
