# Diary-Self RabbitMQ 迁移至 RocketMQ 方案

## 1. 文档目的

本文档整理 Diary-Self 项目中 RabbitMQ 与 RocketMQ 的讨论结论，用于指导后续将核心异步功能逐步从 RabbitMQ 迁移至 RocketMQ。

总体目标：

- RocketMQ 承担核心异步任务、领域事件、事务消息、延时消息和顺序消息。
- XXL-Job 继续承担 Cron、周期扫描和补偿任务，不用 MQ 替代调度系统。
- diary-notify 继续作为统一通知中心，通过 WebSocket/Netty 向用户推送消息。
- RabbitMQ 仅在迁移阶段临时保留，所有链路迁移并清空存量消息后退出项目。

## 2. 当前项目现状

### 2.1 RabbitMQ 已有使用

RabbitMQ 当前已经实际参与以下链路：

1. diary-file 在 OSS 上传成功后发送消息。
2. diary-file 消费上传成功消息并更新数据库中的文件状态。
3. diary-file 使用手动 ACK、生产者 Confirm、Return Callback 和死信队列。
4. diary-notify 已实现 `notify.queue` 消费入口，设计目标是接收各业务模块的通知，然后进行在线 WebSocket 推送或离线消息存储。

主要相关代码：

- `diary-config/src/main/java/diary/config/mqconfig/RabbitMqConfig.java`
- `diary-file/src/main/java/diary/file/impl/VideoFileServiceImpl.java`
- `diary-file/src/main/java/diary/file/impl/asyncserviceImpl/AsyncServiceImpl.java`
- `diary-file/src/main/java/diary/file/impl/MqConsumerServiceImpl.java`
- `diary-notify/src/main/java/diary/notify/consumer/NotifyMessageConsumer.java`

### 2.2 RocketMQ 已有使用

项目父 POM 已统一管理 `rocketmq-v5-client-spring-boot-starter`，diary-file 也已经引入该依赖，但当前仓库中尚未发现 RocketMQ 生产者、消费者或 Listener 实现。

因此，目前 RocketMQ 处于“已引入依赖、尚未真正使用”的状态。

## 3. 最终技术选型

项目后续以 RocketMQ 作为统一的核心消息中间件。

| 技术 | 最终职责 |
| --- | --- |
| RocketMQ | 异步任务、领域事件、事务消息、延时消息、顺序消息、消费重试和死信 |
| XXL-Job | Cron 调度、周期扫描、数据补偿和批处理任务 |
| diary-notify | 消费通知事件，完成在线 WebSocket 推送和离线消息存储 |
| RabbitMQ | 迁移期间临时兼容，完成迁移后退出 |

选择 RocketMQ 的主要原因：

- 更适合后续核心业务事件化和异步化。
- 原生支持事务消息。
- 支持延时消息和 FIFO 顺序消息。
- 适合消息堆积、消费重试、死信和事件回放场景。
- 与国内 Java 微服务项目的常见技术栈更接近，有利于后续扩展和技术积累。

## 4. 各模块可使用 RocketMQ 的场景

### 4.1 diary-AI

适用场景：

- 提交 AI 分析任务。
- 异步调用大模型。
- 异步生成分析报告。
- AI 调用失败重试。
- 发布 AI 分析完成或失败事件。
- 通知 diary-notify 向用户推送 `AI_COMPLETE`。

建议：作为第一个 RocketMQ 迁移或试点模块。AI 请求通常耗时较长，与异步任务模型天然匹配，而且与当前 RabbitMQ 文件链路相互独立，试点风险较低。

### 4.2 diary-notify

适用场景：

- 消费目标到期通知。
- 消费目标进度落后通知。
- 消费饮食提醒。
- 消费 AI 分析完成通知。
- 消费文件处理完成通知。
- 消费 XXL-Job 执行完成或失败通知。
- 将消息推送给在线用户，或者保存成离线消息。

当前已经定义的通知类型包括：

- `GOAL_DUE`
- `GOAL_PROGRESS`
- `DIET_REMIND`
- `TASK_COMPLETE`
- `AI_COMPLETE`
- `FILE_READY`

建议：将当前 RabbitMQ `notify.queue` 消费模型迁移为 RocketMQ 的统一通知 Topic 消费者。

### 4.3 diary-goal

适用场景：

- 目标创建、修改、完成和删除领域事件。
- 阶段目标到期提醒。
- 目标进度落后提醒。
- 目标导出异步处理。
- 目标事件触发通知、审计或统计。

建议：目标到期和进度检查继续由 XXL-Job 定时扫描；扫描得到结果后，将通知事件发送到 RocketMQ，再由 diary-notify 消费。

对于同一个目标有严格状态顺序要求的场景，可以使用 FIFO Topic，并使用 `goalId` 作为消息组。

### 4.4 diary-diet

适用场景：

- 饭点提醒。
- 长时间未记录饮食提醒。
- 饮食记录创建和修改事件。
- 饮食统计数据异步更新。
- 向 diary-notify 发布 `DIET_REMIND`。

建议：固定饭点和周期性检查由 XXL-Job 负责，RocketMQ 负责可靠传输通知事件。

### 4.5 diary-file

适用场景：

- OSS 上传任务。
- OSS 上传完成或失败事件。
- 图片压缩、水印、缩略图生成。
- 视频转码和封面提取。
- 文件异步删除。
- 文件状态顺序更新。
- 向 diary-notify 发布 `FILE_READY`。

建议：该模块最后迁移。原因是当前 RabbitMQ 上传完成、手动 ACK、生产确认和死信链路已经存在，迁移涉及面和风险相对较大。

如果文件状态必须严格按照“创建 → 上传中 → 处理完成 → 删除”的顺序消费，可以使用 FIFO Topic，并以 `fileId` 作为消息组。

注意：MQ 消息中只传递 `fileId`、object key、任务类型等必要信息，不直接传输图片、视频或其他大文件。

### 4.6 diary-user

适用场景：

- 用户注册成功事件。
- 用户注销事件。
- 用户资料变更事件。
- 欢迎通知。
- 注销后的跨模块数据清理。

用户注册或注销如果需要同时可靠触发多个下游操作，可以考虑 RocketMQ 事务消息；也可以使用本地事务加 Outbox 事件表。

消息中不得传递明文密码、Token 或其他敏感认证信息。

### 4.7 diary-recipe

适用场景：

- 菜谱新增、修改和删除事件。
- 缓存失效通知。
- 推荐数据异步更新。
- 搜索索引异步同步。
- 审计记录。

当前业务量下使用普通消息即可，无需使用 FIFO 或事务消息。

### 4.8 diary-timemachine

适用场景：

- 时光卡片创建、修改和删除事件。
- 周年或纪念日提醒。
- 内容归档。
- 搜索索引和统计数据异步更新。

周年提醒可能被用户修改或取消，优先使用 XXL-Job 扫描后发送普通消息，避免提前很长时间投递延时消息所带来的取消和重排问题。

### 4.9 diary-xxljob

适用场景：

- 天气任务执行完成通知。
- 图片清理任务执行完成通知。
- 定时任务失败告警。
- 批处理结果事件。

职责边界：

```text
XXL-Job：决定任务什么时候执行
RocketMQ：负责将执行结果可靠地发送给下游
```

### 4.10 不建议直接接入的模块

- diary-gateway：网关不应承担业务消息生产和消费职责。
- diary-config：只负责公共配置，不应成为业务消息消费者。
- diary-common：只放公共消息模型、事件协议和常量。
- diary-utils：只放无业务状态的通用工具。

## 5. Topic、Tag 和 Consumer Group 规划

RocketMQ 5.x 中，一个 Topic 应保持明确的消息类型。普通、FIFO、Delay 和 Transaction 消息不要随意混入同一个 Topic。

### 5.1 Topic 初步规划

| Topic | 消息类型 | 生产模块 | 消费模块 | 用途 |
| --- | --- | --- | --- | --- |
| `diary-ai-task` | Normal | diary-AI/API入口 | diary-AI Worker | AI 分析和报告任务 |
| `diary-ai-event` | Normal | diary-AI | diary-notify、审计消费者 | AI 完成或失败事件 |
| `diary-notify-event` | Normal | 各业务模块、XXL-Job | diary-notify | 统一实时通知 |
| `diary-reminder-delay` | Delay | diary-goal、diary-diet | diary-notify/提醒消费者 | 短期且不易修改的一次性提醒 |
| `diary-file-task` | Normal | diary-file | 文件处理 Worker | 上传后处理、删除、转码等任务 |
| `diary-file-event` | Normal | diary-file Worker | diary-file、diary-notify | 文件完成或失败事件 |
| `diary-file-status-fifo` | FIFO | diary-file | diary-file | 同一文件的顺序状态更新 |
| `diary-user-transaction` | Transaction | diary-user | 通知和清理消费者 | 用户核心事务事件 |
| `diary-domain-event` | Normal | goal、diet、recipe、timemachine | 审计、缓存、统计消费者 | 通用领域事件 |

Topic 不应一开始拆得过细。只有当消息类型、权限、保存周期、吞吐量或消费者模型不同的时候才拆分 Topic。

### 5.2 Tag 规划

以统一通知 Topic 为例：

```text
Topic: diary-notify-event

Tags:
  GOAL_DUE
  GOAL_PROGRESS
  DIET_REMIND
  TASK_COMPLETE
  TASK_FAILED
  AI_COMPLETE
  AI_FAILED
  FILE_READY
  FILE_FAILED
```

Tag 用于区分同一 Topic 下的事件子类型，消费者可以按 Tag 订阅和过滤。

### 5.3 Message Key 规划

每条消息至少设置：

- `eventId`：事件唯一标识，也是首选 Message Key。
- `eventType`：事件类型。
- `occurredAt`：事件发生时间。
- `producer`：生产模块。
- `schemaVersion`：消息结构版本。
- `userId`：与用户相关时携带。
- `aggregateId`：目标、文件、AI 任务等领域对象 ID。

业务定位时可以使用以下 Key：

- `aiTaskId`
- `goalId`
- `fileId`
- `userId`
- `jobId`

### 5.4 Consumer Group 规划

不同业务目的必须使用不同 Consumer Group；同一业务的多个实例使用相同 Consumer Group 进行负载均衡。

示例：

```text
diary-notify-push-group
diary-notify-offline-group
diary-ai-worker-group
diary-file-process-group
diary-file-status-group
diary-audit-group
```

如果同一条事件既要推送通知又要写审计记录，应使用两个不同 Consumer Group，使两个业务各自收到一份消息。

## 6. 消息模型建议

统一消息信封示例：

```json
{
  "eventId": "01HXXX...",
  "eventType": "AI_COMPLETE",
  "schemaVersion": 1,
  "producer": "diary-AI",
  "occurredAt": 1750000000000,
  "userId": 10001,
  "aggregateId": "ai-task-123",
  "traceId": "trace-xxx",
  "payload": {
    "taskId": "ai-task-123",
    "resultId": 90001
  }
}
```

消息设计原则：

- 消息表达已经发生的事实，例如 `FILE_READY`，而不是含义模糊的 `handleFile`。
- 只传递必要字段和小型业务快照。
- 不传输 MultipartFile、图片、视频、PDF、Excel 或大段 AI 输出。
- 大内容存入数据库或 OSS，消息只携带 ID 和 object key。
- 消息结构发生不兼容变化时递增 `schemaVersion`。
- 不在消息中传递密码、JWT、AccessKey、SecretKey 等敏感信息。

## 7. 可靠性规范

无论迁移前后，都必须遵守以下规范。

### 7.1 生产端可靠性

- 同步发送成功只表示 Broker 接受消息，不代表消费者已经完成业务。
- 核心事件使用 RocketMQ 事务消息或者数据库 Outbox。
- 普通异步任务发送失败时记录失败状态，并由补偿任务重新发送。
- 记录 Topic、Tag、Key、eventId、发送结果和异常原因。
- 不依赖应用内存无限重试。

### 7.2 消费端幂等

RocketMQ 提供至少一次投递语义，重复消息是正常情况，所有核心消费者必须幂等。

推荐使用下列方式之一：

1. 建立消息消费记录表，对 `consumerGroup + eventId` 设置唯一索引。
2. 使用业务表状态机，例如只有 `UPLOADING` 才能更新为 `READY`。
3. 对天然唯一的业务操作使用数据库唯一约束。

不能仅依赖 Redis 短期锁作为永久幂等依据。

### 7.3 重试与死信

- 短暂的网络、数据库和第三方服务故障可以重试。
- 参数错误、数据不存在等永久性错误不应无限重试。
- 设置有限重试次数和合理退避时间。
- 超过重试次数后进入死信队列。
- 死信消息需要支持查询、告警、修复和人工重新投递。
- 消费者不得吞掉异常后返回成功。

### 7.4 顺序消息

- 只在确实要求顺序的业务中使用 FIFO。
- 使用 `fileId`、`goalId`、`userId` 等作为消息组。
- 顺序保证只发生在同一个消息组内，不追求全局顺序。
- 消费者失败会阻塞同消息组后续消息，因此消费逻辑必须短小、稳定。

### 7.5 延时消息

适合：

- 短期、一次性、时间确定且不容易取消的提醒。
- 失败后的延迟补偿。
- 短时间后的状态检查。

不适合：

- 每天、每周等长期周期任务。
- 可能频繁修改或取消的目标截止时间。
- 需要复杂日历规则的提醒。

周期和可变提醒继续使用 XXL-Job 扫描，扫描完成后发送 RocketMQ 普通消息。

## 8. 分阶段迁移计划

### 阶段一：建立公共规范

- 在 diary-common 中定义统一消息信封和领域事件 DTO。
- 确定 Topic、Tag、Message Key 和 Consumer Group 命名规范。
- 建立消息幂等表或统一幂等组件。
- 建立发送、消费、重试和死信监控。
- 在 Nacos 中统一管理 RocketMQ 地址、认证和 Consumer Group 配置。

### 阶段二：diary-AI 试点

- 将 AI 调用改造成提交任务和异步消费。
- 保存 AI 任务状态：`PENDING`、`RUNNING`、`SUCCESS`、`FAILED`。
- AI 完成后发布 `AI_COMPLETE`。
- diary-notify 消费完成事件并推送用户。
- 验证生产重试、消费重试、幂等和死信流程。

### 阶段三：统一通知链路

- 创建 `diary-notify-event`。
- diary-notify 接入 RocketMQ 消费者。
- goal、diet、AI、file、XXL-Job 分批切换通知生产端。
- 确保在线推送失败时能够保存离线消息。
- 停止 RabbitMQ `notify.queue` 的新消息生产。
- 消费完 RabbitMQ 存量通知消息后关闭旧消费者。

### 阶段四：目标、饮食和定时任务

- XXL-Job 扫描目标和饮食提醒。
- 扫描结果发送到 RocketMQ。
- diary-notify 统一消费提醒。
- 对重复扫描产生的相同提醒进行幂等控制。

### 阶段五：迁移文件处理链路

- 建立 RocketMQ 文件任务和文件事件 Topic。
- 实现 RocketMQ 文件消费者。
- 对同一文件的状态更新增加状态机和幂等控制。
- 先切换一个独立文件场景验证，再迁移 OSS 上传完成主链路。
- 停止 RabbitMQ OSS 上传消息生产。
- 消费完 `oss.upload.queue` 和死信队列中的存量消息。
- 关闭 RabbitMQ 文件消费者。

### 阶段六：核心领域事件

- 用户注册、注销接入事务消息或 Outbox。
- goal、recipe、timemachine 发布标准领域事件。
- 按需要增加审计、缓存失效、统计和搜索索引消费者。

### 阶段七：RabbitMQ 退役

- 确认所有 RabbitMQ 生产者已经停止。
- 确认普通队列、重试队列和死信队列没有存量消息。
- 保留一段观察期。
- 删除 RabbitMQ Listener、配置和依赖。
- 停止 RabbitMQ 服务和监控。
- 更新部署文档与故障处理手册。

## 9. 单条链路的安全迁移步骤

每条业务链路按照以下顺序迁移：

```text
1. 创建 RocketMQ Topic 和 Consumer Group
2. 实现并启动 RocketMQ 消费者
3. 验证消费者幂等、重试和死信
4. 将生产者切换到 RocketMQ
5. 停止向 RabbitMQ 发送新消息
6. 等待 RabbitMQ 普通、重试和死信队列处理完成
7. 关闭 RabbitMQ 旧消费者
8. 观察稳定后删除旧代码和配置
```

不建议让生产者长期同时向 RabbitMQ 和 RocketMQ 双写。双写会产生发送结果不一致、重复消费和排障困难。

如果迁移期间必须并行验证，优先使用消息桥接或者只读影子消费者，并确保影子消费者不修改真实业务数据。

## 10. 优先级总结

建议实施顺序：

```text
公共消息规范与可靠性基础
        ↓
diary-AI 异步任务试点
        ↓
diary-notify 统一通知链路
        ↓
diary-goal / diary-diet / XXL-Job
        ↓
diary-file OSS 与文件处理链路
        ↓
diary-user 等核心领域事件
        ↓
RabbitMQ 清空并退役
```

核心原则：先完成幂等、重试、死信、监控和消息追踪，再扩大 RocketMQ 使用范围；不要只替换客户端 API，而忽略消息一致性和可运维性。

## 11. 参考资料

- [Apache RocketMQ：消息模型](https://rocketmq.apache.org/docs/domainModel/05message/)
- [Apache RocketMQ：事务消息](https://rocketmq.apache.org/docs/featureBehavior/04transactionmessage/)
- [Apache RocketMQ：FIFO 顺序消息](https://rocketmq.apache.org/docs/featureBehavior/03fifomessage/)
- [Apache RocketMQ：消费重试](https://rocketmq.apache.org/docs/featureBehavior/10consumerretrypolicy/)
- [Apache RocketMQ：发送重试与限流](https://rocketmq.apache.org/docs/featureBehavior/05sendretrypolicy/)

