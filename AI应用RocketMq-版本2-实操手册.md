# AI 应用 RocketMQ——版本 2 实操手册

> 本手册参照《AI 应用 RocketMQ——版本 2（单实例微服务实践：Outbox + Redis）》编写，并以 Diary-Self 当前代码为起点。目标是让你按步骤完成代码，而不是一次性复制一套与现有工程脱节的新架构。

## 1. 本手册的最终交付

完成后，diary-AI 应具备：

1. `ai_task` 与 `mq_outbox` 在同一个本地事务中创建。
2. HTTP 提交接口只等待 MySQL 提交，不等待 RocketMQ。
3. 单实例 Outbox Publisher 自动发送、失败退避、超时恢复。
4. RocketMQ Consumer 继续使用第一版的原子抢占、租约、`workerId + versionId` 和有限重试。
5. Redis 缓存任务状态，并加速 `clientRequestId` 幂等查询。
6. Redis 对单用户提交次数做基础计数。
7. 本地 `Semaphore` 控制当前 diary-AI 进程的 Qwen Plus 并发。
8. 定时任务恢复租约过期的 `RUNNING` 任务。
9. 提供任务状态和结果查询接口。
10. 成功或失败事件也可通过 Outbox 可靠发送给 diary-notify。

本版只运行一个 diary-AI 实例，不实现：

- Redis 分布式锁。
- Redis 分布式信号量。
- 多实例 Outbox 抢占。
- 全局限流和集群 Token 预算。
- RocketMQ 事务消息。

## 2. 当前代码起点与先修问题

当前第一版已经存在：

```text
AiTaskApplicationServiceImpl
AiTaskProducer
AiTaskConsumer
AiTaskExecutor
DatabaseServiceImpl
DiaryAiMapper / DiaryAIMapper.xml
AiTaskPO / AiTaskMessageDto / AiTaskProcessDto
```

当前需要先注意两个问题：

1. `DiaryAIController` 已调用 `getTaskStatus()` 和 `getTaskResult()`，但 `AiTaskApplicationService` 尚未声明这两个方法。
2. `AiTaskQueryService` 已创建但还是空接口。

不要把查询方法继续塞进提交服务。建议恢复以下职责：

```text
AiTaskApplicationService：只负责提交任务
AiTaskQueryService：负责状态和结果查询
```

Controller 最终注入两个 Service。

## 3. 推荐的最终目录

```text
diary-common
└── src/main/java/diary/common
    ├── consts/ai
    │   ├── AiTaskConstants.java
    │   └── AiTaskErrorCode.java
    └── entity/ai
        ├── enums
        │   ├── AiTaskStatus.java
        │   ├── OutboxStatus.java
        │   └── OutboxEventType.java
        ├── po
        │   ├── AiTaskPO.java                 # 已存在
        │   └── MqOutboxPO.java               # 新增
        ├── dto
        │   ├── AiTaskMessageDto.java         # 已存在
        │   └── AiTaskEventDto.java           # 新增
        └── vo
            ├── AiTaskSubmitVo.java           # 已存在
            ├── AiTaskStatusVo.java           # 新增
            └── AiTaskResultVo.java           # 新增

diary-AI
└── src/main/java/diary/diaryai
    ├── controller/DiaryAIController.java
    ├── properties/AiTaskProperties.java
    ├── service
    │   ├── AiTaskApplicationService.java
    │   ├── AiTaskQueryService.java
    │   ├── AiTaskCommandService.java
    │   ├── AiOutboxService.java
    │   └── OutboxMessageProducer.java
    ├── impl
    │   ├── AiTaskApplicationServiceImpl.java
    │   ├── AiTaskQueryServiceImpl.java
    │   ├── AiTaskCommandServiceImpl.java
    │   └── AiOutboxServiceImpl.java
    ├── outbox
    │   └── AiOutboxPublisher.java
    ├── redis
    │   ├── AiRedisKeyFactory.java
    │   ├── AiTaskCacheService.java
    │   ├── AiIdempotencyCacheService.java
    │   └── AiSubmitRateLimiter.java
    ├── guard
    │   └── LocalAiConcurrencyGuard.java
    ├── recovery
    │   ├── AiTaskRecoveryJob.java
    │   └── AiTaskRecoveryService.java
    ├── rocketmqhandler
    │   ├── producer/RocketMqOutboxProducer.java
    │   └── consumer/AiTaskConsumer.java
    └── mapper/DiaryAiMapper.java
```

如果不想一次调整过多包名，可以先保留现有类的位置。重要的是职责和事务边界正确，不是目录必须完全一致。

## 4. 第一步：恢复可编译的接口契约

### 4.1 保持提交接口单一职责

`AiTaskApplicationService.java` 保持：

```java
package diary.diaryai.service;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.vo.AiTaskSubmitVo;

public interface AiTaskApplicationService {
    AiTaskSubmitVo submitTask(AiInvokeDTO aiInvokeDTO);
}
```

### 4.2 补全查询接口

```java
package diary.diaryai.service;

import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;

public interface AiTaskQueryService {
    AiTaskStatusVo getTaskStatus(Long taskId);

    AiTaskResultVo getTaskResult(Long taskId);
}
```

### 4.3 暂时修正 Controller

在查询 Service 尚未实现前，可以先注释当前两个 GET 接口，保证代码可编译；完成第十二步后再恢复。也可以先创建一个抛出 `UnsupportedOperationException` 的临时实现，但不要提交到最终版本。

接口路径最终统一为：

```text
POST /ai/tasks
GET  /ai/tasks/{taskId}
GET  /ai/tasks/{taskId}/result
```

当前 `/ai/tasks/result/{taskId}` 应改成 `/ai/tasks/{taskId}/result`。

### 4.4 本步验证

- Controller 不再调用接口中不存在的方法。
- `AiTaskQueryService` 有明确返回类型。
- 第一版提交和消费代码未改动。

## 5. 第二步：添加枚举和静态常量

不要继续在多个类中写 `"PENDING"`、`"QWEN_PLUS_NUTRIENT"` 和 `3`。

### 5.1 任务状态枚举

```java
package diary.common.entity.ai.enums;

public enum AiTaskStatus {
    PENDING,
    QUEUED,
    RUNNING,
    RETRY_WAIT,
    SUCCESS,
    FAILED,
    CANCELLED,
    DEAD_LETTER;

    public boolean isTerminal() {
        return this == SUCCESS
                || this == FAILED
                || this == CANCELLED
                || this == DEAD_LETTER;
    }
}
```

数据库字段暂时仍使用 `VARCHAR`，Mapper 参数使用 `status.name()`。不要在这一版同时改 MyBatis Enum TypeHandler。

### 5.2 Outbox 状态枚举

```java
package diary.common.entity.ai.enums;

public enum OutboxStatus {
    NEW,
    SENDING,
    RETRY_WAIT,
    SENT,
    DEAD
}
```

### 5.3 Outbox 事件类型

```java
package diary.common.entity.ai.enums;

public enum OutboxEventType {
    AI_TASK_CREATED,
    AI_COMPLETED,
    AI_FAILED
}
```

### 5.4 AI 任务静态常量

```java
package diary.common.consts.ai;

public final class AiTaskConstants {
    private AiTaskConstants() {
    }

    public static final long FIRST_VERSION_USER_ID = 10000L;
    public static final String TASK_TYPE_QWEN_PLUS_NUTRIENT = "QWEN_PLUS_NUTRIENT";
    public static final int MESSAGE_SCHEMA_VERSION = 1;
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final int MAX_ERROR_MESSAGE_LENGTH = 1000;
    public static final String EVENT_ID_PREFIX = "evt-";
    public static final String AGGREGATE_TYPE_AI_TASK = "AI_TASK";
}
```

运行时可能调整的值最终应放入 Nacos 配置。这里的数值是缺省值和协议常量，不要把 Topic、Redis TTL、扫描周期放进这个类。

### 5.5 错误码常量

```java
package diary.common.consts.ai;

public final class AiTaskErrorCode {
    private AiTaskErrorCode() {
    }

    public static final String PERMANENT_ERROR = "AI_PERMANENT_ERROR";
    public static final String RETRYABLE_ERROR = "AI_RETRYABLE_ERROR";
    public static final String RETRY_EXHAUSTED = "AI_RETRY_EXHAUSTED";
    public static final String SNAPSHOT_INVALID = "AI_SNAPSHOT_INVALID";
    public static final String SUBMIT_RATE_LIMITED = "AI_SUBMIT_RATE_LIMITED";
    public static final String OUTBOX_SEND_FAILED = "AI_OUTBOX_SEND_FAILED";
}
```

提交限流建议使用一个能直接映射 HTTP 429 的异常：

```java
package diary.diaryai.exception;

import diary.common.consts.ai.AiTaskErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class AiSubmitRateLimitException extends RuntimeException {
    public AiSubmitRateLimitException() {
        super(AiTaskErrorCode.SUBMIT_RATE_LIMITED);
    }
}
```

如果项目已有统一异常处理器，则不要使用 `@ResponseStatus`，改为在全局异常处理器中把该异常映射成 HTTP 429 和稳定业务错误码。

### 5.6 替换当前硬编码

至少替换：

```text
AiTaskApplicationServiceImpl.FIRST_VERSION_USER_ID
AiTaskApplicationServiceImpl.TASK_TYPE
AiTaskApplicationServiceImpl.MAX_ATTEMPTS
AiTaskConsumer.SUPPORTED_SCHEMA_VERSION
AiTaskConsumer.TASK_TYPE
AiTaskConsumer.MAX_ERROR_MESSAGE_LENGTH
AiTaskConsumer 中所有任务状态字符串判断
```

SQL 中的状态字符串暂时保留，因为 XML 直接写状态更直观。

## 6. 第三步：添加实体类和查询 VO

### 6.1 `MqOutboxPO`

在 `diary-common/src/main/java/diary/common/entity/ai/po` 新增：

```java
package diary.common.entity.ai.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqOutboxPO {
    private Long id;
    private String eventId;
    private String aggregateType;
    private Long aggregateId;
    private String eventType;
    private String topic;
    private String tag;
    private String messageKey;
    private String payload;
    private Integer schemaVersion;
    private String status;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime nextRetryTime;
    private String brokerMessageId;
    private String lastError;
    private LocalDateTime sentTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer versionId;
}
```

不要把 `payload` 定义成 `Object`。Outbox 保存的是已经序列化并固定下来的消息 JSON，重发时不应重新拼业务数据。

### 6.2 `AiTaskEventDto`

成功和失败事件共用一个最小 DTO：

```java
package diary.common.entity.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskEventDto {
    private String eventId;
    private String eventType;
    private Long taskId;
    private Long userId;
    private Long resultId;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime occurTime;
    private Integer schemaVersion;
    private String traceId;
}
```

`AI_COMPLETED` 使用 `resultId`，`AI_FAILED` 使用错误字段。不要把完整营养结果和输入快照塞入事件。

### 6.3 `AiTaskStatusVo`

```java
package diary.common.entity.ai.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiTaskStatusVo {
    private Long taskId;
    private String status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private Long resultId;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime queueTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private Integer versionId;
}
```

缓存任务状态时就序列化这个 VO，不缓存 `AiTaskPO`，避免把 `inputSnapshot` 和 `workerId` 放入 Redis。

### 6.4 `AiTaskResultVo`

```java
package diary.common.entity.ai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiTaskResultVo {
    private Long taskId;
    private String status;
    private Long aiInfoId;
    private Long universalId;
    private String flag;
    private String calory;
    private String protein;
    private String fat;
    private String carbohydrate;
    private String sugar;
    private String sodium;
    private String errorCode;
    private String errorMessage;
}
```

第二版先沿用当前营养字段的 `String` 类型，不在本次改造中同时处理单位标准化。

## 7. 第四步：执行数据库变更

### 7.1 先检查重复结果

当前 `ai_nutrient.ai_task_id` 只有普通索引。添加唯一索引前先检查：

```sql
SELECT ai_task_id, COUNT(*) AS cnt
FROM ai_nutrient
GROUP BY ai_task_id
HAVING COUNT(*) > 1;
```

如果有重复数据，先人工确认保留哪一条，再添加唯一索引：

```sql
ALTER TABLE ai_nutrient
    ADD UNIQUE KEY uk_ai_nutrient_task_id (ai_task_id);
```

这是结果幂等的数据库最后防线。

### 7.2 给任务恢复扫描增加索引

```sql
ALTER TABLE ai_task
    ADD KEY idx_ai_task_status_lease (status, lease_until),
    ADD KEY idx_ai_task_status_create (status, create_time);
```

如果索引已存在，不要重复执行。

### 7.3 创建 Outbox 表

```sql
CREATE TABLE mq_outbox (
    id                BIGINT UNSIGNED NOT NULL COMMENT '主键',
    event_id          VARCHAR(64)     NOT NULL COMMENT '事件幂等ID',
    aggregate_type    VARCHAR(32)     NOT NULL COMMENT '聚合类型',
    aggregate_id      BIGINT UNSIGNED NOT NULL COMMENT '当前为taskId',
    event_type        VARCHAR(64)     NOT NULL COMMENT '事件类型',
    topic             VARCHAR(128)    NOT NULL COMMENT 'RocketMQ Topic',
    tag               VARCHAR(64)     NOT NULL COMMENT 'RocketMQ Tag',
    message_key       VARCHAR(128)    NOT NULL COMMENT 'RocketMQ Message Key',
    payload           JSON            NOT NULL COMMENT '固定消息JSON',
    schema_version    INT             NOT NULL DEFAULT 1,
    status            VARCHAR(16)     NOT NULL COMMENT 'Outbox状态',
    retry_count       INT             NOT NULL DEFAULT 0,
    max_retries       INT             NOT NULL DEFAULT 10,
    next_retry_time   DATETIME(3)     NOT NULL,
    broker_message_id VARCHAR(128)    NULL,
    last_error        VARCHAR(1000)   NULL,
    sent_time         DATETIME(3)     NULL,
    create_time       DATETIME(3)     NOT NULL,
    update_time       DATETIME(3)     NOT NULL,
    version_id        INT UNSIGNED    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mq_outbox_event_id (event_id),
    KEY idx_mq_outbox_publish (status, next_retry_time, update_time),
    KEY idx_mq_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息Outbox表';
```

### 7.4 本步验证 SQL

```sql
SHOW INDEX FROM ai_task;
SHOW INDEX FROM ai_nutrient;
SHOW CREATE TABLE mq_outbox;
```

## 8. 第五步：添加依赖和配置属性

### 8.1 `diary-AI/pom.xml`

直接声明 Redis 依赖，不要只依赖其他业务模块传递进来：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>diary</groupId>
    <artifactId>diary-config</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>diary</groupId>
    <artifactId>diary-utils</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

当前 `CallAIServiceImpl` 仍引用 `diary.file.service.downloadservice.DownloadService`，所以这一阶段不要直接删除 `diary-file` 依赖。等旧同步接口完全下线后再单独解耦。

### 8.2 创建配置类

```java
package diary.diaryai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "diary.ai")
public class AiTaskProperties {
    private Rocketmq rocketmq = new Rocketmq();
    private Task task = new Task();
    private Cache cache = new Cache();
    private Limit limit = new Limit();

    @Data
    public static class Rocketmq {
        private String taskTopic = "diary-ai-task";
        private String taskTag = "QWEN_PLUS_NUTRIENT";
        private String taskConsumerGroup = "diary-ai-qwen-plus-worker-v2";
        private String eventTopic = "diary-ai-event";
        private int publisherBatchSize = 20;
        private long publisherIntervalMs = 1000;
        private long publisherSendingTimeoutSeconds = 60;
        private int outboxMaxRetries = 10;
    }

    @Data
    public static class Task {
        private int maxAttempts = 3;
        private long executionLeaseSeconds = 330;
        private long recoveryIntervalMs = 30000;
    }

    @Data
    public static class Cache {
        private long runningTtlSeconds = 30;
        private long terminalTtlHours = 24;
        private long idempotencyTtlHours = 24;
        private long nullTtlSeconds = 15;
        private String keyPrefix = "diary:dev:ai";
    }

    @Data
    public static class Limit {
        private int submitPerUserPerMinute = 10;
        private int modelLocalConcurrency = 2;
        private long localPermitWaitMs = 1000;
    }
}
```

### 8.3 Nacos 配置

```yaml
diary:
  ai:
    rocketmq:
      task-topic: diary-ai-task
      task-tag: QWEN_PLUS_NUTRIENT
      task-consumer-group: diary-ai-qwen-plus-worker-v2
      event-topic: diary-ai-event
      publisher-batch-size: 20
      publisher-interval-ms: 1000
      publisher-sending-timeout-seconds: 60
      outbox-max-retries: 10
    task:
      max-attempts: 3
      execution-lease-seconds: 330
      recovery-interval-ms: 30000
    cache:
      running-ttl-seconds: 30
      terminal-ttl-hours: 24
      idempotency-ttl-hours: 24
      null-ttl-seconds: 15
      key-prefix: diary:dev:ai
    limit:
      submit-per-user-per-minute: 10
      model-local-concurrency: 2
      local-permit-wait-ms: 1000
```

Redis 的连接地址、密码和连接池继续使用 Spring 标准的 `spring.data.redis` 配置。

### 8.4 开启定时任务

```java
@SpringBootApplication
@EnableScheduling
public class DiaryAIApplication {
    // main 保持不变
}
```

只需要 `@EnableScheduling`，不要给 RocketMQ Listener 再加 `@Async`。

## 9. 第六步：扩展 Mapper

### 9.1 Mapper 接口新增方法

在现有 `DiaryAiMapper` 中追加：

```java
int insertOutbox(MqOutboxPO outbox);

List<MqOutboxPO> selectReadyOutbox(
        @Param("now") LocalDateTime now,
        @Param("limit") int limit
);

MqOutboxPO selectOutboxById(@Param("id") Long id);

int claimOutbox(
        @Param("id") Long id,
        @Param("versionId") Integer versionId,
        @Param("updateTime") LocalDateTime updateTime
);

int markOutboxSent(
        @Param("id") Long id,
        @Param("versionId") Integer versionId,
        @Param("brokerMessageId") String brokerMessageId,
        @Param("sentTime") LocalDateTime sentTime
);

int markOutboxRetry(
        @Param("id") Long id,
        @Param("versionId") Integer versionId,
        @Param("nextRetryTime") LocalDateTime nextRetryTime,
        @Param("lastError") String lastError,
        @Param("updateTime") LocalDateTime updateTime
);

int markOutboxDead(
        @Param("id") Long id,
        @Param("versionId") Integer versionId,
        @Param("lastError") String lastError,
        @Param("updateTime") LocalDateTime updateTime
);

int recoverSendingTimeout(
        @Param("timeoutBefore") LocalDateTime timeoutBefore,
        @Param("now") LocalDateTime now
);

int markQueuedByTaskIdIfPending(
        @Param("taskId") Long taskId,
        @Param("queueTime") LocalDateTime queueTime
);

List<AiTaskPO> selectExpiredRunningTasks(
        @Param("now") LocalDateTime now,
        @Param("limit") int limit
);

AiNutrientPO selectAiNutrientByTaskId(@Param("taskId") Long taskId);
```

记得补充 `List`、`LocalDateTime`、`MqOutboxPO` 导入。

### 9.2 Outbox ResultMap

```xml
<resultMap id="MqOutboxResultMap" type="diary.common.entity.mq.po.MqOutboxPO">
    <id column="id" property="id"/>
    <result column="event_id" property="eventId"/>
    <result column="aggregate_type" property="aggregateType"/>
    <result column="aggregate_id" property="aggregateId"/>
    <result column="event_type" property="eventType"/>
    <result column="topic" property="topic"/>
    <result column="tag" property="tag"/>
    <result column="message_key" property="messageKey"/>
    <result column="payload" property="payload"/>
    <result column="schema_version" property="schemaVersion"/>
    <result column="status" property="status"/>
    <result column="retry_count" property="retryCount"/>
    <result column="max_retries" property="maxRetries"/>
    <result column="next_retry_time" property="nextRetryTime"/>
    <result column="broker_message_id" property="brokerMessageId"/>
    <result column="last_error" property="lastError"/>
    <result column="sent_time" property="sentTime"/>
    <result column="create_time" property="createTime"/>
    <result column="update_time" property="updateTime"/>
    <result column="version_id" property="versionId"/>
</resultMap>
```

### 9.3 核心 Outbox SQL

```xml
<insert id="insertOutbox" parameterType="diary.common.entity.mq.po.MqOutboxPO">
    INSERT INTO mq_outbox
        (id, event_id, aggregate_type, aggregate_id, event_type,
         topic, tag, message_key, payload, schema_version,
         status, retry_count, max_retries, next_retry_time,
         create_time, update_time, version_id)
    VALUES
        (#{id}, #{eventId}, #{aggregateType}, #{aggregateId}, #{eventType},
         #{topic}, #{tag}, #{messageKey}, #{payload}, #{schemaVersion},
         #{status}, #{retryCount}, #{maxRetries}, #{nextRetryTime},
         #{createTime}, #{updateTime}, #{versionId})
</insert>

<select id="selectReadyOutbox" resultMap="MqOutboxResultMap">
    SELECT *
    FROM mq_outbox
    WHERE status IN ('NEW', 'RETRY_WAIT')
      AND next_retry_time &lt;= #{now}
    ORDER BY next_retry_time ASC, id ASC
    LIMIT #{limit}
</select>

<select id="selectOutboxById" resultMap="MqOutboxResultMap">
    SELECT * FROM mq_outbox WHERE id = #{id} LIMIT 1
</select>

<update id="claimOutbox">
    UPDATE mq_outbox
    SET status = 'SENDING',
        update_time = #{updateTime},
        version_id = version_id + 1
    WHERE id = #{id}
      AND status IN ('NEW', 'RETRY_WAIT')
      AND version_id = #{versionId}
</update>

<update id="markOutboxSent">
    UPDATE mq_outbox
    SET status = 'SENT',
        broker_message_id = #{brokerMessageId},
        sent_time = #{sentTime},
        update_time = #{sentTime},
        last_error = NULL,
        version_id = version_id + 1
    WHERE id = #{id}
      AND status = 'SENDING'
      AND version_id = #{versionId}
</update>

<update id="markOutboxRetry">
    UPDATE mq_outbox
    SET status = 'RETRY_WAIT',
        retry_count = retry_count + 1,
        next_retry_time = #{nextRetryTime},
        last_error = #{lastError},
        update_time = #{updateTime},
        version_id = version_id + 1
    WHERE id = #{id}
      AND status = 'SENDING'
      AND version_id = #{versionId}
</update>

<update id="markOutboxDead">
    UPDATE mq_outbox
    SET status = 'DEAD',
        retry_count = retry_count + 1,
        last_error = #{lastError},
        update_time = #{updateTime},
        version_id = version_id + 1
    WHERE id = #{id}
      AND status = 'SENDING'
      AND version_id = #{versionId}
</update>

<update id="recoverSendingTimeout">
    UPDATE mq_outbox
    SET status = 'RETRY_WAIT',
        next_retry_time = #{now},
        last_error = 'SENDING_TIMEOUT_RECOVERED',
        update_time = #{now},
        version_id = version_id + 1
    WHERE status = 'SENDING'
      AND update_time &lt; #{timeoutBefore}
</update>
```

### 9.4 任务与结果查询 SQL

```xml
<update id="markQueuedByTaskIdIfPending">
    UPDATE ai_task
    SET status = 'QUEUED',
        queue_time = COALESCE(queue_time, #{queueTime}),
        version_id = version_id + 1
    WHERE id = #{taskId}
      AND status = 'PENDING'
</update>

<select id="selectExpiredRunningTasks" resultMap="AiTaskResultMap">
    SELECT *
    FROM ai_task
    WHERE status = 'RUNNING'
      AND lease_until &lt; #{now}
    ORDER BY lease_until ASC
    LIMIT #{limit}
</select>

<select id="selectAiNutrientByTaskId" resultMap="AiNutrientResultMap">
    SELECT *
    FROM ai_nutrient
    WHERE ai_task_id = #{taskId}
    LIMIT 1
</select>
```

当前 `AiNutrientResultMap` 还应补上：

```xml
<result column="flag" property="flag" jdbcType="VARCHAR"/>
```

## 10. 第七步：实现“任务 + Outbox”本地事务

### 10.1 Command Service 接口

```java
public interface AiTaskCommandService {
    AiTaskPO createTaskAndOutbox(AiInvokeDTO request, Long userId);
}
```

### 10.2 事务实现

```java
@Service
@RequiredArgsConstructor
public class AiTaskCommandServiceImpl implements AiTaskCommandService {
    private final DiaryAiMapper diaryAiMapper;
    private final ObjectMapper objectMapper;
    private final AiTaskProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiTaskPO createTaskAndOutbox(AiInvokeDTO request, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Long taskId = MyUtils.getPrimaryKey();
        String eventId = AiTaskConstants.EVENT_ID_PREFIX + MyUtils.getPrimaryKey();

        String inputSnapshot = writeJson(request, "AI任务输入快照序列化失败");

        AiTaskPO task = AiTaskPO.builder()
                .id(taskId)
                .userId(userId)
                .clientRequestId(request.getClientRequestId())
                .taskType(AiTaskConstants.TASK_TYPE_QWEN_PLUS_NUTRIENT)
                .status(AiTaskStatus.PENDING.name())
                .inputSnapshot(inputSnapshot)
                .attemptCount(0)
                .maxAttempts(properties.getTask().getMaxAttempts())
                .createTime(now)
                .versionId(0)
                .build();

        AiTaskMessageDto message = AiTaskMessageDto.builder()
                .eventId(eventId)
                .taskId(taskId)
                .userId(userId)
                .clientRequestId(request.getClientRequestId())
                .taskType(AiTaskConstants.TASK_TYPE_QWEN_PLUS_NUTRIENT)
                .schemaVersion(AiTaskConstants.MESSAGE_SCHEMA_VERSION)
                .occurTime(now)
                .traceId(MDC.get("traceId"))
                .build();

        MqOutboxPO outbox = MqOutboxPO.builder()
                .id(MyUtils.getPrimaryKey())
                .eventId(eventId)
                .aggregateType(AiTaskConstants.AGGREGATE_TYPE_AI_TASK)
                .aggregateId(taskId)
                .eventType(OutboxEventType.AI_TASK_CREATED.name())
                .topic(properties.getRocketmq().getTaskTopic())
                .tag(properties.getRocketmq().getTaskTag())
                .messageKey(taskId.toString())
                .payload(writeJson(message, "AI任务消息序列化失败"))
                .schemaVersion(AiTaskConstants.MESSAGE_SCHEMA_VERSION)
                .status(OutboxStatus.NEW.name())
                .retryCount(0)
                .maxRetries(properties.getRocketmq().getOutboxMaxRetries())
                .nextRetryTime(now)
                .createTime(now)
                .updateTime(now)
                .versionId(0)
                .build();

        if (diaryAiMapper.insertAiTask(task) != 1) {
            throw new IllegalStateException("创建AI任务失败");
        }
        if (diaryAiMapper.insertOutbox(outbox) != 1) {
            throw new IllegalStateException("创建AI任务Outbox失败");
        }
        return task;
    }

    private String writeJson(Object value, String message) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(message, e);
        }
    }
}
```

### 10.3 为什么必须拆成另一个 Bean

不要在 `AiTaskApplicationServiceImpl` 内写一个 private `@Transactional` 方法再自行调用。Spring 事务代理不会拦截同类内部调用。

正确调用关系：

```text
AiTaskApplicationServiceImpl
        ↓ 调用另一个 Spring Bean
AiTaskCommandServiceImpl.createTaskAndOutbox()
        ↓ @Transactional
insert ai_task + insert mq_outbox
```

### 10.4 改造提交服务

新的 `submitTask()` 先不要接 Redis，先完成 Outbox 主链路：

```java
@Override
public AiTaskSubmitVo submitTask(AiInvokeDTO request) {
    validateAndNormalizeRequest(request);
    Long userId = AiTaskConstants.FIRST_VERSION_USER_ID;

    AiTaskPO existing = diaryAiMapper.selectByUserIdAndClientRequestId(
            userId, request.getClientRequestId());
    if (existing != null) {
        return toSubmitVo(existing, "该请求已提交");
    }

    try {
        AiTaskPO task = aiTaskCommandService.createTaskAndOutbox(request, userId);
        return toSubmitVo(task, "AI分析任务已受理");
    } catch (DuplicateKeyException e) {
        AiTaskPO concurrent = diaryAiMapper.selectByUserIdAndClientRequestId(
                userId, request.getClientRequestId());
        if (concurrent == null) {
            throw e;
        }
        return toSubmitVo(concurrent, "该请求已提交");
    }
}
```

删除以下旧逻辑：

- `AiTaskMessageProducer` 字段。
- `rocketMqHandlerService.send()`。
- HTTP 线程中的 `SendReceipt` 处理。
- HTTP 线程中的 `markQueuedIfPending()`。

提交成功时返回 `PENDING` 是第二版的正确行为。

### 10.5 本步验证

暂时关闭 Outbox Publisher，调用提交接口后检查：

```sql
SELECT id, status FROM ai_task ORDER BY create_time DESC LIMIT 1;
SELECT event_id, aggregate_id, status FROM mq_outbox ORDER BY create_time DESC LIMIT 1;
```

预期：

```text
ai_task.status = PENDING
mq_outbox.status = NEW
```

主动让 `insertOutbox` 抛异常，验证 `ai_task` 也被回滚。

## 11. 第八步：实现通用 RocketMQ Outbox Producer

### 11.1 Producer 接口

```java
public interface OutboxMessageProducer {
    SendReceipt send(MqOutboxPO outbox);
}
```

### 11.2 Producer 实现

```java
@Service
@RequiredArgsConstructor
public class RocketMqOutboxProducer implements OutboxMessageProducer {
    private final RocketMQClientTemplate rocketMQClientTemplate;

    @Override
    public SendReceipt send(MqOutboxPO outbox) {
        Message<String> message = MessageBuilder
                .withPayload(outbox.getPayload())
                .setHeader(RocketMQHeaders.KEYS, outbox.getMessageKey())
                .build();

        String destination = outbox.getTopic() + ":" + outbox.getTag();
        return rocketMQClientTemplate.syncSendNormalMessage(destination, message);
    }
}
```

Outbox 中已经保存序列化后的 payload，所以这里不再反序列化、重新构造消息。

原 `AiTaskProducer` 和 `AiTaskMessageProducer` 可以在主链路切换完成后删除，避免出现两条发送入口。

## 12. 第九步：实现 Outbox 状态事务和 Publisher

### 12.1 Outbox Service

```java
public interface AiOutboxService {
    boolean claim(MqOutboxPO outbox);

    void confirmSent(MqOutboxPO sendingOutbox, String brokerMessageId);

    void recordFailure(MqOutboxPO sendingOutbox, Throwable error);

    int recoverSendingTimeout();
}
```

### 12.2 状态实现关键点

`claim()` 成功后，数据库 `version_id` 已经 `+1`。因此 Publisher 后续使用的对象版本也必须加一：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public boolean claim(MqOutboxPO outbox) {
    int changed = diaryAiMapper.claimOutbox(
            outbox.getId(), outbox.getVersionId(), LocalDateTime.now());
    if (changed == 1) {
        outbox.setStatus(OutboxStatus.SENDING.name());
        outbox.setVersionId(outbox.getVersionId() + 1);
        return true;
    }
    return false;
}
```

发送确认事务：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void confirmSent(MqOutboxPO outbox, String brokerMessageId) {
    LocalDateTime now = LocalDateTime.now();
    int sent = diaryAiMapper.markOutboxSent(
            outbox.getId(), outbox.getVersionId(), brokerMessageId, now);
    if (sent != 1) {
        throw new IllegalStateException("Outbox SENT 更新失败: " + outbox.getId());
    }

    if (OutboxEventType.AI_TASK_CREATED.name().equals(outbox.getEventType())) {
        diaryAiMapper.markQueuedByTaskIdIfPending(outbox.getAggregateId(), now);
    }
}
```

`markQueuedByTaskIdIfPending()` 更新为 0 不一定是错误：Consumer 可能已经从 `PENDING` 抢占到 `RUNNING`。不能把实际状态倒退回 `QUEUED`。

失败退避：

```java
private LocalDateTime calculateNextRetry(int currentRetryCount) {
    long baseSeconds = Math.min(1L << Math.min(currentRetryCount, 7), 120L) * 5L;
    long jitterSeconds = ThreadLocalRandom.current().nextLong(0, 4);
    return LocalDateTime.now().plusSeconds(
            Math.min(baseSeconds, 600L) + jitterSeconds);
}
```

完整的失败分支可以写成：

```java
@Override
@Transactional(rollbackFor = Exception.class)
public void recordFailure(MqOutboxPO outbox, Throwable error) {
    LocalDateTime now = LocalDateTime.now();
    int nextRetryCount = outbox.getRetryCount() + 1;
    String lastError = truncate(error.getMessage(), 1000);

    int changed;
    if (nextRetryCount >= outbox.getMaxRetries()) {
        changed = diaryAiMapper.markOutboxDead(
                outbox.getId(), outbox.getVersionId(), lastError, now);
    } else {
        changed = diaryAiMapper.markOutboxRetry(
                outbox.getId(),
                outbox.getVersionId(),
                calculateNextRetry(outbox.getRetryCount()),
                lastError,
                now);
    }
    if (changed != 1) {
        throw new IllegalStateException("Outbox失败状态更新失败: " + outbox.getId());
    }
}

@Override
@Transactional(rollbackFor = Exception.class)
public int recoverSendingTimeout() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime timeoutBefore = now.minusSeconds(
            properties.getRocketmq().getPublisherSendingTimeoutSeconds());
    return diaryAiMapper.recoverSendingTimeout(timeoutBefore, now);
}
```

`truncate()` 应处理 null，并把错误摘要截断为 1000 字符。Outbox 失败状态更新异常要继续抛出并告警，不能把数据库更新失败伪装成已经进入重试。

### 12.3 Publisher

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AiOutboxPublisher {
    private final DiaryAiMapper diaryAiMapper;
    private final AiOutboxService aiOutboxService;
    private final OutboxMessageProducer producer;
    private final AiTaskProperties properties;

    @Scheduled(fixedDelayString = "${diary.ai.rocketmq.publisher-interval-ms:1000}")
    public void publishReadyMessages() {
        aiOutboxService.recoverSendingTimeout();

        List<MqOutboxPO> batch = diaryAiMapper.selectReadyOutbox(
                LocalDateTime.now(),
                properties.getRocketmq().getPublisherBatchSize());

        for (MqOutboxPO outbox : batch) {
            if (!aiOutboxService.claim(outbox)) {
                continue;
            }
            try {
                SendReceipt receipt = producer.send(outbox);
                aiOutboxService.confirmSent(
                        outbox, receipt.getMessageId().toString());
            } catch (RuntimeException e) {
                log.error("Outbox发送失败, outboxId={}, eventId={}",
                        outbox.getId(), outbox.getEventId(), e);
                aiOutboxService.recordFailure(outbox, e);
            }
        }
    }
}
```

### 12.4 重要边界

- 领取 Outbox：短事务。
- 网络发送 RocketMQ：事务外。
- 写 SENT/RETRY_WAIT：短事务。
- Broker 成功但写 SENT 失败：Outbox 留在 SENDING，超时后重发。
- 重发沿用原 `eventId`、`messageKey` 和 `payload`。

### 12.5 本步验证

1. 正常发送：`NEW → SENDING → SENT`，任务 `PENDING → QUEUED`。
2. 停止 Broker：Outbox 进入 `RETRY_WAIT`，`retry_count` 增加。
3. 手工把 Outbox 改成过期 `SENDING`：下一轮恢复为 `RETRY_WAIT`。
4. Broker 恢复：消息自动发出，不需要重新调用 HTTP。

## 13. 第十步：实现 Redis Key 和状态缓存

### 13.1 KeyFactory

```java
@Component
@RequiredArgsConstructor
public class AiRedisKeyFactory {
    private final AiTaskProperties properties;

    public String task(Long taskId) {
        return properties.getCache().getKeyPrefix() + ":task:" + taskId;
    }

    public String idempotency(Long userId, String clientRequestId) {
        return properties.getCache().getKeyPrefix()
                + ":idem:" + userId + ":" + requestHash(clientRequestId);
    }

    public String submitRate(Long userId, long epochMinute) {
        return properties.getCache().getKeyPrefix()
                + ":submit:rate:" + userId + ":" + epochMinute;
    }

    private String requestHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }
}
```

### 13.2 状态缓存 Service

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskCacheService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiRedisKeyFactory keyFactory;
    private final AiTaskProperties properties;

    public Optional<AiTaskStatusVo> get(Long taskId) {
        try {
            String json = redisTemplate.opsForValue().get(keyFactory.task(taskId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, AiTaskStatusVo.class));
        } catch (Exception e) {
            log.warn("读取AI任务缓存失败, taskId={}", taskId, e);
            return Optional.empty();
        }
    }

    public void put(AiTaskStatusVo value) {
        try {
            boolean terminal = AiTaskStatus.valueOf(value.getStatus()).isTerminal();
            Duration ttl = terminal
                    ? Duration.ofHours(properties.getCache().getTerminalTtlHours())
                    : Duration.ofSeconds(properties.getCache().getRunningTtlSeconds());
            long jitter = ThreadLocalRandom.current().nextLong(0, 5);
            redisTemplate.opsForValue().set(
                    keyFactory.task(value.getTaskId()),
                    objectMapper.writeValueAsString(value),
                    ttl.plusSeconds(jitter));
        } catch (Exception e) {
            log.warn("写入AI任务缓存失败, taskId={}", value.getTaskId(), e);
        }
    }

    public void evict(Long taskId) {
        try {
            redisTemplate.delete(keyFactory.task(taskId));
        } catch (RuntimeException e) {
            log.warn("删除AI任务缓存失败, taskId={}", taskId, e);
        }
    }
}
```

缓存失败只影响性能，不能抛异常回滚已经成功的 MySQL 事务。

### 13.3 幂等映射缓存

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AiIdempotencyCacheService {
    private final StringRedisTemplate redisTemplate;
    private final AiRedisKeyFactory keyFactory;
    private final AiTaskProperties properties;

    public Optional<Long> get(Long userId, String clientRequestId) {
        try {
            String value = redisTemplate.opsForValue().get(
                    keyFactory.idempotency(userId, clientRequestId));
            return value == null ? Optional.empty() : Optional.of(Long.valueOf(value));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public void put(Long userId, String clientRequestId, Long taskId) {
        try {
            redisTemplate.opsForValue().set(
                    keyFactory.idempotency(userId, clientRequestId),
                    taskId.toString(),
                    Duration.ofHours(properties.getCache().getIdempotencyTtlHours()));
        } catch (RuntimeException e) {
            log.warn("写入AI幂等缓存失败, taskId={}", taskId, e);
        }
    }

    public void evict(Long userId, String clientRequestId) {
        redisTemplate.delete(keyFactory.idempotency(userId, clientRequestId));
    }
}
```

Redis 命中后必须回查 MySQL：

```text
Redis taskId 命中
    ↓
MySQL 按 taskId 查询
    ├── 存在：返回
    └── 不存在：删除脏缓存，继续查询 userId + clientRequestId
```

Redis 不是提交幂等的最终依据。

## 14. 第十一步：实现 Redis 提交计数

### 14.1 Lua 脚本

```java
private static final DefaultRedisScript<Long> RATE_SCRIPT =
        new DefaultRedisScript<>("""
                local current = redis.call('INCR', KEYS[1])
                if current == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return current
                """, Long.class);
```

### 14.2 RateLimiter

```java
@Service
@RequiredArgsConstructor
public class AiSubmitRateLimiter {
    private final StringRedisTemplate redisTemplate;
    private final AiRedisKeyFactory keyFactory;
    private final AiTaskProperties properties;

    public boolean allow(Long userId) {
        long epochMinute = Instant.now().getEpochSecond() / 60;
        String key = keyFactory.submitRate(userId, epochMinute);
        Long current = redisTemplate.execute(
                RATE_SCRIPT,
                List.of(key),
                "120");
        return current != null
                && current <= properties.getLimit().getSubmitPerUserPerMinute();
    }
}
```

调用顺序必须是：

```text
1. Redis 幂等映射查询
2. MySQL 幂等查询
3. 确认是新任务后再执行提交计数
4. 创建 task + Outbox
```

重复查询同一个 `clientRequestId` 不消耗新任务额度。

Redis 不可用时，本练习建议选择一个清晰策略：

- 开发环境：记录告警并临时放行。
- 模拟生产：拒绝新任务并返回服务暂时不可用，保护模型费用。

不要在 catch 中静默吞掉异常而又不记录任何日志。

## 15. 第十二步：接入 Redis 后改造提交服务

推荐的最终提交顺序：

```java
public AiTaskSubmitVo submitTask(AiInvokeDTO request) {
    validateAndNormalizeRequest(request);
    Long userId = AiTaskConstants.FIRST_VERSION_USER_ID;
    String clientRequestId = request.getClientRequestId();

    Optional<Long> cachedTaskId = idempotencyCache.get(userId, clientRequestId);
    if (cachedTaskId.isPresent()) {
        AiTaskPO cachedTask = diaryAiMapper.selectAiTaskByTaskId(cachedTaskId.get());
        if (cachedTask != null) {
            return toSubmitVo(cachedTask, "该请求已提交");
        }
        idempotencyCache.evict(userId, clientRequestId);
    }

    AiTaskPO existing = diaryAiMapper.selectByUserIdAndClientRequestId(
            userId, clientRequestId);
    if (existing != null) {
        idempotencyCache.put(userId, clientRequestId, existing.getId());
        return toSubmitVo(existing, "该请求已提交");
    }

    if (!submitRateLimiter.allow(userId)) {
        throw new AiSubmitRateLimitException();
    }

    try {
        AiTaskPO created = aiTaskCommandService.createTaskAndOutbox(request, userId);
        idempotencyCache.put(userId, clientRequestId, created.getId());
        taskCache.put(toStatusVo(created));
        return toSubmitVo(created, "AI分析任务已受理");
    } catch (DuplicateKeyException e) {
        AiTaskPO concurrent = diaryAiMapper.selectByUserIdAndClientRequestId(
                userId, clientRequestId);
        if (concurrent == null) {
            throw e;
        }
        idempotencyCache.put(userId, clientRequestId, concurrent.getId());
        return toSubmitVo(concurrent, "该请求已提交");
    }
}
```

注意：以上缓存写入发生在 `createTaskAndOutbox()` 正常返回之后，此时数据库事务已经提交。

## 16. 第十三步：实现查询 Service 和 Controller

### 16.1 状态查询

```java
@Service
@RequiredArgsConstructor
public class AiTaskQueryServiceImpl implements AiTaskQueryService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskCacheService taskCache;

    @Override
    public AiTaskStatusVo getTaskStatus(Long taskId) {
        Optional<AiTaskStatusVo> cached = taskCache.get(taskId);
        if (cached.isPresent()) {
            return cached.get();
        }

        AiTaskPO task = diaryAiMapper.selectAiTaskByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("AI任务不存在: " + taskId);
        }

        AiTaskStatusVo result = toStatusVo(task);
        taskCache.put(result);
        return result;
    }

    @Override
    public AiTaskResultVo getTaskResult(Long taskId) {
        AiTaskPO task = diaryAiMapper.selectAiTaskByTaskId(taskId);
        if (task == null) {
            throw new IllegalArgumentException("AI任务不存在: " + taskId);
        }

        if (!AiTaskStatus.SUCCESS.name().equals(task.getStatus())) {
            return AiTaskResultVo.builder()
                    .taskId(taskId)
                    .status(task.getStatus())
                    .errorCode(task.getErrorCode())
                    .errorMessage(task.getErrorMessage())
                    .build();
        }

        AiNutrientPO nutrient = diaryAiMapper.selectAiNutrientByTaskId(taskId);
        if (nutrient == null) {
            throw new IllegalStateException("SUCCESS任务缺少营养结果: " + taskId);
        }

        return toResultVo(task, nutrient);
    }
}
```

`toStatusVo()` 和 `toResultVo()` 使用普通私有映射方法即可，不需要为这一版额外引入 MapStruct。

### 16.2 Controller

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class DiaryAIController {
    private final AiTaskApplicationService applicationService;
    private final AiTaskQueryService queryService;

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<AiTaskSubmitVo>> submit(
            @RequestBody AiInvokeDTO request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(applicationService.submitTask(request)));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<AiTaskStatusVo>> status(
            @PathVariable Long taskId) {
        return ResponseEntity.ok(
                ApiResponse.success(queryService.getTaskStatus(taskId)));
    }

    @GetMapping("/tasks/{taskId}/result")
    public ResponseEntity<ApiResponse<AiTaskResultVo>> result(
            @PathVariable Long taskId) {
        AiTaskResultVo result = queryService.getTaskResult(taskId);
        HttpStatus status = AiTaskStatus.SUCCESS.name().equals(result.getStatus())
                ? HttpStatus.OK
                : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status)
                .body(ApiResponse.success(result));
    }
}
```

如果任务已经 `FAILED`，可以返回 HTTP 200 + 业务状态，也可以由统一异常处理返回约定错误码。关键是全项目保持一致，不要让 Controller 直接返回异常堆栈。

## 17. 第十四步：在状态迁移后失效缓存

所有任务状态发生变化后都要失效缓存：

```text
Outbox 确认发送：PENDING → QUEUED
Consumer 抢占：QUEUED/RETRY_WAIT → RUNNING
Consumer 可重试失败：RUNNING → RETRY_WAIT
Consumer 终止失败：RUNNING → FAILED
结果事务：RUNNING → SUCCESS
恢复任务：RUNNING → RETRY_WAIT/FAILED
```

最简单的第二版实现：数据库方法成功返回后调用 `taskCache.evict(taskId)`。

对事务方法，必须在事务提交后删除缓存。可以使用：

```java
public final class AfterCommitExecutor {
    private AfterCommitExecutor() {
    }

    public static void run(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
    }
}
```

事务方法内：

```java
AfterCommitExecutor.run(() -> taskCache.evict(taskId));
```

如果暂时不想封装，可以在事务 Service 正常返回后由外层调用方删除缓存；但要在代码注释中明确提交顺序。

## 18. 第十五步：实现本地模型并发保护

### 18.1 Guard

```java
@Component
public class LocalAiConcurrencyGuard {
    private final Semaphore semaphore;
    private final long waitMs;

    public LocalAiConcurrencyGuard(AiTaskProperties properties) {
        this.semaphore = new Semaphore(
                properties.getLimit().getModelLocalConcurrency(), true);
        this.waitMs = properties.getLimit().getLocalPermitWaitMs();
    }

    public boolean tryAcquire() {
        try {
            return semaphore.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release() {
        semaphore.release();
    }
}
```

### 18.2 Consumer 注解和租约配置化

将当前写死的 Consumer Group、Topic 和 Tag 改成配置占位符：

```java
@RocketMQMessageListener(
        consumerGroup = "${diary.ai.rocketmq.task-consumer-group:diary-ai-qwen-plus-worker-v2}",
        topic = "${diary.ai.rocketmq.task-topic:diary-ai-task}",
        tag = "${diary.ai.rocketmq.task-tag:QWEN_PLUS_NUTRIENT}"
)
```

删除 Consumer 内的 `EXECUTION_LEASE = Duration.ofMinutes(5)`，创建抢占参数时使用：

```java
LocalDateTime leaseUntil = now.plusSeconds(
        properties.getTask().getExecutionLeaseSeconds());
```

Consumer Group 从 v1 改为 v2 会产生一套新的消费进度。正式切换前要明确新 Group 的初始消费位置，并在测试 Topic 验证，避免误消费历史测试消息。

### 18.3 Consumer 接入位置

本地许可建议放在数据库抢占之前：

```text
解析消息
    ↓
获取本地许可
    ↓ 成功
数据库原子抢占并增加 attemptCount
    ↓
调用模型
    ↓
finally 释放本地许可
```

这样没有获得许可时不会把 `attemptCount` 增加，也不会让任务提前进入 `RUNNING`。

伪代码：

```java
if (!concurrencyGuard.tryAcquire()) {
    log.warn("本地AI并发已满, taskId={}", message.getTaskId());
    return ConsumeResult.FAILURE;
}

try {
    // 原有 claimForExecution、所有权确认、execute、失败分类逻辑
} finally {
    concurrencyGuard.release();
}
```

同时将 RocketMQ Consumer 并发配置为 1～2，避免大量消息已经交给 Consumer 后只在本地等待。

注意：不要使用 `@Async` 把模型调用扔到后台后提前返回消费成功。

## 19. 第十六步：实现 RUNNING 租约恢复

### 19.1 Recovery Service

恢复状态和创建补发 Outbox 必须在同一个事务中：

```java
public interface AiTaskRecoveryService {
    void recover(AiTaskPO task);
}
```

```java
@Service
@RequiredArgsConstructor
public class AiTaskRecoveryServiceImpl implements AiTaskRecoveryService {
    private final DiaryAiMapper diaryAiMapper;
    private final ObjectMapper objectMapper;
    private final AiTaskProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recover(AiTaskPO task) {
        LocalDateTime now = LocalDateTime.now();

        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            AiTaskProcessDto failed = AiTaskProcessDto.builder()
                    .taskId(task.getId())
                    .userId(task.getUserId())
                    .clientRequestId(task.getClientRequestId())
                    .versionId(task.getVersionId())
                    .finishTime(now)
                    .errorCode(AiTaskErrorCode.RETRY_EXHAUSTED)
                    .errorMessage("RUNNING租约过期且尝试次数已耗尽")
                    .build();
            int changed = diaryAiMapper.markFailedIfAttemptsExhausted(failed);
            if (changed != 1) {
                return;
            }
            insertFailedEventOutbox(task, now);
            return;
        }

        AiTaskProcessDto retry = AiTaskProcessDto.builder()
                .taskId(task.getId())
                .versionId(task.getVersionId())
                .leaseUntil(now)
                .errorCode(AiTaskErrorCode.RETRYABLE_ERROR)
                .errorMessage("RUNNING租约过期，等待恢复")
                .build();

        if (diaryAiMapper.recoverExpiredRunning(retry) != 1) {
            return;
        }
        insertRetryTaskOutbox(task, now);
    }
}
```

`insertRetryTaskOutbox()` 构造新的 `eventId`，但沿用原 `taskId`、`userId`、`clientRequestId` 和 `taskType`。状态使用 `NEW`，Topic/Tag 与任务创建消息相同。

### 19.2 Job

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskRecoveryJob {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskRecoveryService recoveryService;
    private final AiTaskCacheService taskCache;

    @Scheduled(fixedDelayString = "${diary.ai.task.recovery-interval-ms:30000}")
    public void recoverExpiredRunning() {
        List<AiTaskPO> tasks = diaryAiMapper.selectExpiredRunningTasks(
                LocalDateTime.now(), 50);

        for (AiTaskPO task : tasks) {
            try {
                recoveryService.recover(task);
                taskCache.evict(task.getId());
            } catch (RuntimeException e) {
                log.error("恢复AI任务失败, taskId={}", task.getId(), e);
            }
        }
    }
}
```

Job 和事务 Service 必须是两个 Bean，避免 `@Transactional` 自调用失效。

### 19.3 重复补发是否安全

RocketMQ 自身重投和 Recovery Job 补发可能同时存在。第一版已有的 `claimForExecution`、终态判断、`workerId + versionId` 和 `ai_task_id` 唯一约束保证重复消息不会产生重复结果。

## 20. 第十七步：把成功/失败事件写入 Outbox

这一步可以在任务主链路稳定后再做。

### 20.1 成功事件

当前 `DatabaseServiceImpl.processData()` 的事务是：

```text
insert AiInfo
insert AiNutrient
mark task SUCCESS
```

第二版追加：

```text
insert mq_outbox(AI_COMPLETED)
```

四项必须在同一个事务中。建议事件 payload：

```json
{
  "eventId": "evt-...",
  "eventType": "AI_COMPLETED",
  "taskId": "2000000000001",
  "userId": 10000,
  "resultId": "3000000000001",
  "occurTime": "2026-08-15T10:00:00+08:00",
  "schemaVersion": 1,
  "traceId": "..."
}
```

不要在事件里放完整营养结果。diary-notify 或前端通过查询接口获取结果。

### 20.2 失败事件

当前 `AiTaskConsumer.handleExecutionFailure()` 直接调用 Mapper 更新 FAILED。为了让 `FAILED + AI_FAILED Outbox` 原子提交，应把失败终态更新提取到独立事务 Service：

```text
update task RUNNING → FAILED
insert mq_outbox(AI_FAILED)
```

可重试的 `RUNNING → RETRY_WAIT` 不发送最终失败事件。

### 20.3 Topic 和 Tag

```text
Topic: diary-ai-event
Tag: AI_COMPLETED / AI_FAILED
Message Key: taskId
```

同一个 `AiOutboxPublisher` 和 `RocketMqOutboxProducer` 可发送任务消息与结果事件，无需再复制一个扫描器。

## 21. 完整编码顺序清单

严格按以下顺序执行，每一步验证通过后再继续：

### 阶段 A：恢复基线

- [ ] 补全 `AiTaskQueryService` 方法签名。
- [ ] 暂时修正 Controller，确保没有调用不存在的方法。
- [ ] 保证第一版提交和 Consumer 行为不变。

### 阶段 B：模型与数据库

- [ ] 新增任务状态、Outbox 状态、事件类型枚举。
- [ ] 新增任务常量和错误码。
- [ ] 新增 `MqOutboxPO`、`AiTaskStatusVo`、`AiTaskResultVo`。
- [ ] 检查重复 `ai_task_id` 后添加结果唯一索引。
- [ ] 创建 `mq_outbox`。
- [ ] 添加任务恢复扫描索引。

### 阶段 C：Outbox 主链路

- [ ] 扩展 Mapper 和 XML。
- [ ] 创建 `AiTaskCommandServiceImpl` 事务。
- [ ] 提交服务移除直接 MQ 发送。
- [ ] 创建通用 Outbox Producer。
- [ ] 创建 Outbox 状态 Service。
- [ ] 创建单实例 Publisher。
- [ ] 验证 Broker 故障和应用重启补发。

### 阶段 D：Redis

- [ ] diary-AI 显式引入 Redis 和 diary-config。
- [ ] 创建 `AiRedisKeyFactory`。
- [ ] 创建任务状态 Cache-Aside。
- [ ] 创建幂等映射缓存。
- [ ] 创建提交计数 Lua。
- [ ] 验证 Redis 清空后系统仍正确。
- [ ] 验证 Redis 不可用时符合选定降级策略。

### 阶段 E：查询和执行保护

- [ ] 实现 `AiTaskQueryServiceImpl`。
- [ ] 修正 Controller 查询路径和返回类型。
- [ ] 在所有状态迁移后失效缓存。
- [ ] 接入本地 `Semaphore`。
- [ ] Consumer 线程数降到 1～2。

### 阶段 F：恢复和事件

- [ ] 创建过期 RUNNING 查询。
- [ ] 创建 Recovery 事务 Service。
- [ ] 创建单实例 Recovery Job。
- [ ] SUCCESS 事务写 `AI_COMPLETED` Outbox。
- [ ] FAILED 事务写 `AI_FAILED` Outbox。
- [ ] diary-notify 做 `consumerGroup + eventId` 幂等。

## 22. 每阶段建议测试

### 22.1 提交事务

| 场景 | 预期 |
| --- | --- |
| 正常提交 | task=PENDING，Outbox=NEW |
| Outbox 插入失败 | task 和 Outbox 都回滚 |
| 重复 clientRequestId | 返回同一 taskId |
| 两个并发重复请求 | 数据库只有一个 task 和一个创建事件 |

### 22.2 Publisher

| 场景 | 预期 |
| --- | --- |
| Broker 正常 | Outbox=SENT，task=QUEUED/RUNNING/终态 |
| Broker 停止 | Outbox=RETRY_WAIT，按退避时间重试 |
| 发送后写 SENT 前宕机 | SENDING 超时后重发，结果不重复 |
| 达到最大次数 | Outbox=DEAD，并产生日志/告警 |

### 22.3 Redis

| 场景 | 预期 |
| --- | --- |
| 首次查状态 | Redis miss，查询 MySQL 并回填 |
| 再次查状态 | Redis hit |
| 状态迁移 | 缓存被删除，下一次读取新状态 |
| Redis 清空 | 自动从 MySQL 重建 |
| 脏幂等映射 | 回查 MySQL 失败后删除脏 Key |
| 超过提交阈值 | 返回限流错误，不创建任务 |

### 22.4 Consumer 与恢复

| 场景 | 预期 |
| --- | --- |
| 多条消息同时到达 | 本进程模型并发不超过 Semaphore 上限 |
| 模型调用抛异常 | 许可在 finally 释放 |
| Worker 在 RUNNING 宕机 | 租约过期后恢复并补发 |
| 最后一次尝试宕机 | 收敛为 FAILED |
| 重复补发 | 不重复调用终态任务，不重复保存结果 |

## 23. 常见实现错误

- 在 `AiTaskApplicationServiceImpl` 内写 private `@Transactional` 方法并自行调用。
- 插入任务后仍在 HTTP 线程直接发送 MQ。
- 把 Redis 写入放在 MySQL 事务提交前。
- Redis 命中 taskId 后不回查 MySQL。
- 缓存完整 `AiTaskPO`，把输入快照写入 Redis。
- Outbox Publisher 在一个长事务里发送整批 MQ。
- 每次 Outbox 重发重新生成 eventId。
- Broker 成功后无条件把任务覆盖成 QUEUED。
- 用普通索引代替 `ai_nutrient.ai_task_id` 唯一索引。
- Recovery Job 直接调用 Producer，不写 Outbox。
- Job 和带事务的恢复方法写在同一个类并自调用。
- 先抢占任务、增加 attemptCount，再长时间等待本地并发许可。
- Listener 把任务提交给 `@Async` 后立即 ACK。
- 同时开启 SDK、业务代码和 RocketMQ 多层高次数重试。
- 把第二版扩成 Redis 分布式锁或多实例信号量，导致无法聚焦微服务主链路。

## 24. 第二版实操完成标准

以下全部满足后，再进入第三版：

1. 项目能够编译，Controller 与 Service 契约一致。
2. `ai_task + mq_outbox` 同事务提交和回滚。
3. HTTP 提交不直接调用 RocketMQ。
4. Outbox 可完成发送、退避、SENDING 超时恢复和 DEAD 收敛。
5. Broker 停机后恢复，无需重新提交 HTTP 请求。
6. 重复 Outbox 消息不产生重复营养结果。
7. 状态查询使用 Redis Cache-Aside。
8. Redis 缓存被清空后，MySQL 可以重建所有查询数据。
9. Redis 幂等映射不替代数据库唯一约束。
10. 单用户提交计数和 HTTP 429 语义可验证。
11. 本地模型并发不超过配置值，异常后许可不泄漏。
12. RUNNING 任务租约过期后可以自动恢复或失败收敛。
13. SUCCESS/FAILED 事件通过 Outbox 可靠发布。
14. 日志能够串联 `taskId + eventId + outboxId + messageId`。
15. 第一版的 `attemptCount`、`versionId`、`workerId + versionId` 和条件状态更新全部保留。

## 25. 第三版的明确起点

第二版完成后，再开始以下分布式实践：

```text
diary-AI 多实例部署
    ↓
多实例 Outbox 抢占与 SKIP LOCKED
    ↓
Redis 分布式调度锁
    ↓
Redis ZSet 全局模型信号量
    ↓
全局限流、缓存一致性和集群费用治理
    ↓
RocketMQ 事务消息与 Outbox 对照实验
```

第三版不是把第二版推倒重做，而是在第二版已经可靠的事务、消息、缓存和恢复边界上增加多实例协调。
