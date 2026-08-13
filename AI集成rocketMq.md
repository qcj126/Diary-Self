# AI 集成 RocketMQ：最新问题与提交幂等设计

## 1. 文档范围

本文档补充 `diary-AI` 接入 RocketMQ 时最新发现的实现问题，并说明 `AiTaskApplicationServiceImpl` 如何使用 `clientRequestId` 实现提交幂等。

当前业务是对已有饮食或食谱的食材、用量、佐料和烹饪方式进行营养分析，不是图片识别。一个 AI 任务对应一个 `flag + universalId`，正常情况下产生一条汇总营养结果。

代码中已经用注释明确标记的原子抢占、状态机、重试、租约恢复等问题，视为已知待办，本文不将它们重复当作新遗漏。

## 2. 最新发现的问题

### 2.1 `@Transactional` 实际不生效

`InvokeQwenPlus.processData()` 是 `private` 方法，并且由同一个对象内部调用。Spring 事务依赖代理拦截，私有方法和类内自调用不会经过代理，因此当前下列操作并没有组成一个真正的本地事务：

```text
insert AiInfoPO
insert AiNutrientPO
update ai_task SUCCESS
```

建议将落库逻辑拆到独立 Spring Bean：

```java
@Service
@RequiredArgsConstructor
public class AiResultPersistenceService {

    private final DiaryAiMapper diaryAiMapper;

    @Transactional(rollbackFor = Exception.class)
    public void saveResult(/* 所需参数 */) {
        // 1. insert AiInfoPO
        // 2. insert AiNutrientPO
        // 3. 条件更新 ai_task -> SUCCESS，并回填 aiInfoId
        // 4. 后续可在这里写 AI_COMPLETED Outbox
    }
}
```

`InvokeQwenPlus` 负责调用模型和解析响应，解析成功后再调用 `AiResultPersistenceService`。不建议把耗时的外部模型调用放进数据库事务。

### 2.2 Prompt 的 JSON 契约互相矛盾

`PromptContext.getNutrientContentByModelQwenPlusAndFlash()` 中同时存在：

- 要求返回 JSON 数组。
- 示例却是单个 JSON 对象。
- `InvokeQwenPlus.extractResult()` 按单个 `Map<String,String>` 解析。

应统一为单个 JSON 对象：

```text
请只返回一个纯净的 JSON 对象，不要返回数组、Markdown 或其他说明文字。
```

推荐响应：

```json
{
  "卡路里": "680 kcal",
  "蛋白质": "42.5 g",
  "脂肪": "31.2 g",
  "碳水化合物": "55.8 g",
  "糖": "6.1 g",
  "钠": "920 mg"
}
```

`universalId` 不需要由模型返回。模型调用只收到 `materials`，并不知道真实 `universalId`；落库时应直接使用任务输入快照中的值。

### 2.3 旧 Prompt 仍包含图片识别语义

`PromptContext.getUniversalNutrientContent()` 仍包含“每张图片”、`uniqueId` 和 JSON 数组等旧描述。

- 如果已无调用方，建议删除。
- 如果其他模型仍在使用，需同步改为食谱/饮食汇总分析语义。

### 2.4 当前只做了 `null` 校验

`MyUtils.check().notNull()` 无法拦截以下数据：

```json
{
  "clientRequestId": "   ",
  "flag": "",
  "materials": {}
}
```

建议补充：

- `clientRequestId` 和 `flag` 先 `trim`，再检查空字符串。
- `materials` 不能是空 Map，内层 Map 也不能为空。
- 限制食材数量、字段长度和总输入长度。
- `flag` 只允许 `DIET` 或 `RECIPE`。
- 本期 `aiType` 只允许 Qwen Plus 对应的枚举值。

### 2.5 输入快照序列化失败时会更新不存在的任务

`inputSnapshot` 在插入 `ai_task` 之前生成。如果序列化失败，当前 catch 会尝试把尚未入库的 `taskId` 更新为 `FAILED`。

建议序列化失败时直接抛出参数或系统异常，不更新任务状态。

### 2.6 RocketMQ 发送异常不在当前 catch 范围内

`AiTaskProducer` 把消息序列化异常包装为 `RuntimeException`，MQ 网络错误通常也是运行时异常，但 `AiTaskApplicationServiceImpl` 只捕获 `JsonProcessingException`。

发送失败时任务保留为 `PENDING` 是合理的，但需要：

- 记录 taskId、eventId 和发送异常。
- 保留 `PENDING`，不应立即改成业务 `FAILED`。
- 实现 PENDING 扫描补偿或 Outbox Publisher。
- 补发也要通过条件抢占，避免多实例重复发送。

### 2.7 MDC 导包错误

当前使用了 RocketMQ shaded 包内的 MDC：

```java
org.apache.rocketmq.shaded.org.slf4j.MDC
```

应改为项目正常的 SLF4J MDC：

```java
org.slf4j.MDC
```

否则可能无法取到 HTTP 链路中设置的 `traceId`。

### 2.8 `AiTaskPO` 部分字段类型不匹配

建议调整：

| 字段 | 当前类型 | 建议类型 |
| --- | --- | --- |
| `leaseUntil` | `String` | `LocalDateTime` 或 `Instant` |
| `aiInfoId` | `String` | `Long` |
| `workerId` | `String` 空串 | 保留 String，未抢占时使用 `null` |
| `errorCode` | `String` 空串 | 保留 String，无错误时使用 `null` |
| `errorMessage` | `String` 空串 | 保留 String，无错误时使用 `null` |

`DiaryAiMapper.updateAiTaskStatus()` 已经使用 `Long aiInfoId`，因此 PO 中也应保持一致。

### 2.9 其他简短问题

- `AiTaskProducer` 中的 `DiaryAiMapper` 已没有用途，可删除。
- RocketMQ Message Key 建议显式使用 `taskId.toString()`。
- `AiNutrientPO` 建议增加 `taskId`，便于建立结果幂等唯一约束。
- 生产环境不建议完整打印 AI 返回结果和用户的全部食谱内容。
- `calory` 英文命名更常见的写法是 `calorie`；如果数据库已经固定，可以后续统一处理。
- `PromptContext` 中的图片类 import 已无用，可删除。

## 3. `clientRequestId` 幂等目标

`clientRequestId` 表示客户端的一次用户操作。前端因为用户双击、HTTP 超时或网络重试重复调用提交接口时，后端应返回第一次已创建的任务，而不是创建新任务。

幂等维度建议为：

```text
userId + clientRequestId
```

当前 `userId` 虽然固定为 `10000L`，仍保留 `userId` 维度，方便后续接入真实用户。

## 4. 数据库唯一约束

仅在 Java 中执行“先查询再插入”不能保证并发幂等。两个请求可能同时查询不到，然后各自插入一条任务。

必须建立数据库唯一索引：

```sql
ALTER TABLE ai_task
ADD UNIQUE KEY uk_ai_task_user_client_request (
    user_id,
    client_request_id
);
```

当前 PO 字段名是 `clientId`，建议改成 `clientRequestId`，数据库列统一命名为 `client_request_id`，避免和客户端应用 ID、设备 ID 混淆。

## 5. Mapper 接口

需要增加按幂等键查询的 Mapper：

```java
AiTaskPO selectByUserIdAndClientRequestId(
        Long userId,
        String clientRequestId
);
```

MyBatis 多参数方法建议显式使用 `@Param`：

```java
AiTaskPO selectByUserIdAndClientRequestId(
        @Param("userId") Long userId,
        @Param("clientRequestId") String clientRequestId
);
```

SQL 语义：

```sql
SELECT *
FROM ai_task
WHERE user_id = #{userId}
  AND client_request_id = #{clientRequestId}
LIMIT 1;
```

`insertAiTask` 建议返回受影响行数：

```java
int insertAiTask(AiTaskPO aiTaskPO);
```

## 6. `AiTaskApplicationServiceImpl` 幂等流程

```text
1. 校验并规范化 clientRequestId
2. 根据 userId + clientRequestId 查询旧任务
3. 如果存在，直接返回旧任务
4. 如果不存在，生成 taskId 并插入 PENDING 任务
5. 如果并发插入触发唯一键冲突，再查询并返回已存在的任务
6. 只有本次真正插入任务的请求才发送 RocketMQ 消息
7. 发送成功后条件更新 PENDING -> QUEUED
8. 发送失败保留 PENDING，由补偿任务处理
```

## 7. 参考代码

```java
@Override
public AiTaskSubmitVo submitTask(AiInvokeDTO request) {
    validateRequest(request);

    final Long userId = 10000L;
    final String clientRequestId = request.getClientRequestId().trim();

    // 快速路径：大部分重复请求可以在这里直接返回。
    AiTaskPO existing = DiaryAiMapper
            .selectByUserIdAndClientRequestId(userId, clientRequestId);
    if (existing != null) {
        return toSubmitVo(existing, "该请求已提交");
    }

    Long taskId = MyUtils.getPrimaryKey();
    String inputSnapshot = serializeInputSnapshot(request);
    AiTaskPO task = buildPendingTask(
            taskId,
            userId,
            clientRequestId,
            request,
            inputSnapshot
    );

    try {
        DiaryAiMapper.insertAiTask(task);
    } catch (DuplicateKeyException e) {
        // 并发窗口：另一个请求已先插入相同幂等键。
        AiTaskPO concurrentTask = DiaryAiMapper
                .selectByUserIdAndClientRequestId(userId, clientRequestId);
        if (concurrentTask == null) {
            throw e;
        }
        return toSubmitVo(concurrentTask, "该请求已提交");
    }

    // 仅首次成功创建任务的请求会走到这里。
    AiTaskMessageDto message = buildTaskMessage(task);
    try {
        SendReceipt receipt = rocketMqHandlerService.send(message);
        int updated = DiaryAiMapper.markQueuedIfPending(
                taskId,
                receipt.getMessageId().toString(),
                LocalDateTime.now()
        );

        // Consumer 可能已经把任务更新为 RUNNING/SUCCESS。
        // updated == 0 不一定是异常，重新查询当前状态即可。
        AiTaskPO current = DiaryAiMapper.selectAiTaskByTaskId(taskId);
        return toSubmitVo(current, "AI分析任务正在处理中");
    } catch (RuntimeException sendException) {
        log.error("AI task message send failed, taskId={}, eventId={}",
                taskId, message.getEventId(), sendException);

        // 不改为 FAILED。任务保持 PENDING，等待补偿发送。
        throw sendException;
    }
}
```

需要导入：

```java
import org.springframework.dao.DuplicateKeyException;
```

`markQueuedIfPending` 的 SQL 必须是条件更新：

```sql
UPDATE ai_task
SET status = 'QUEUED',
    queued_at = #{queuedAt},
    rocketmq_message_id = #{messageId},
    version = version + 1
WHERE task_id = #{taskId}
  AND status = 'PENDING';
```

不能使用不带前置状态的通用 `updateAiTaskStatus`，否则 Consumer 已经把任务更新为 `RUNNING` 或 `SUCCESS` 后，Producer 仍可能将它覆盖回 `QUEUED`。

## 8. 重复请求不应再次发送 MQ

查询到已有任务时，直接返回当前任务信息：

```java
private AiTaskSubmitVo toSubmitVo(AiTaskPO task, String message) {
    return AiTaskSubmitVo.builder()
            .taskId(task.getTaskId())
            .status(task.getStatus())
            .message(message)
            .build();
}
```

即使已有任务是 `PENDING`，也不建议由重复 HTTP 请求直接补发，否则多个重复请求可能同时发送消息。`PENDING` 任务应由独立补偿发布器通过条件抢占处理。

## 9. 相同 `clientRequestId` 但请求内容不同

前端可能错误复用同一个 `clientRequestId`：

```text
第一次：猪肉 300g
第二次：牛肉 500g
clientRequestId 相同
```

如果只比较 `clientRequestId`，第二次请求会直接得到第一次任务。更完整的方案是在 `ai_task` 增加 `request_hash`。

哈希输入应使用规范化后的业务字段：

```text
requestHash = SHA-256(
    aiType
    + aiApplication
    + flag
    + universalId
    + 规范化后的 materials JSON
)
```

处理规则：

| 情况 | 处理 |
| --- | --- |
| `clientRequestId` 相同，`requestHash` 相同 | 返回原任务 |
| `clientRequestId` 相同，`requestHash` 不同 | 返回 HTTP 409，提示幂等键被不同请求复用 |

生成哈希前需要对 `materials` 做稳定序列化，例如按 key 排序，否则内容相同但 Map 顺序不同时可能生成不同哈希。

第一版如果暂时不增加 `requestHash`，至少要在接口约定中明确：同一 `clientRequestId` 只能对应完全相同的请求内容。

## 10. 事务边界注意事项

不建议用一个长数据库事务包裹“插入任务 + 发送 RocketMQ”：

```text
数据库插入成功
    ↓
Broker 收到消息
    ↓
本地事务回滚
```

这会导致 Consumer 收到消息却查不到任务。

第一版保持：

```text
先可靠插入 PENDING 任务
再发送 MQ
发送成功后条件更新 QUEUED
发送失败由 PENDING 补偿
```

后续使用 Outbox 时，在同一本地事务中插入 `ai_task` 和 `mq_outbox`，由 Outbox Publisher 独立发送消息。

## 11. 验证用例

| 场景 | 预期结果 |
| --- | --- |
| 同一 `clientRequestId` 串行请求两次 | 两次返回同一 taskId |
| 同一 `clientRequestId` 并发请求 | 数据库只有一条任务 |
| 并发插入唯一键冲突 | 失败方查询并返回已有任务 |
| 重复 HTTP 请求 | 不再次主动发送 MQ |
| 首次 MQ 发送失败 | 任务保持 PENDING，后续补发 |
| Broker 已收到但 QUEUED 更新前宕机 | 补发可能产生重复消息，Consumer 幂等保证结果不重复 |
| 相同幂等键、相同 requestHash | 返回原任务 |
| 相同幂等键、不同 requestHash | 返回 409，不创建任务 |

## 12. 实施顺序

1. 修正 Prompt 的单 JSON 对象契约。
2. 将结果落库拆到独立事务 Bean。
3. 统一 `AiTaskPO` 字段类型和空值。
4. 增加 `(user_id, client_request_id)` 唯一索引。
5. 增加幂等查询 Mapper 和并发唯一键冲突处理。
6. 保证只有首次创建任务的请求发送 MQ。
7. 将 `PENDING -> QUEUED` 改成条件更新。
8. 实现 PENDING 补偿发送。
9. 有需要时再增加 `request_hash` 和 409 冲突检查。
