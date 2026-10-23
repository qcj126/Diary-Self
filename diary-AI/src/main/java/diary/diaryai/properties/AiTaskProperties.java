package diary.diaryai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "diary.ai")
public class AiTaskProperties {
    @Valid
    private Rocketmq rocketmq = new Rocketmq();
    @Valid
    private Task task = new Task();
    @Valid
    private Cache redis = new Cache();
    @Valid
    private Limit limit = new Limit();

    @Data
    public static class Rocketmq {
        private String completedTag;
        private String failedTag;
        private String taskTopic;
        private String taskNutrientTag;
        private String taskSentimentTag;
        private String aiTaskConsumerTag;
        private String aiTaskConsumerGroup;
        private String taskConsumerSentimentGroup;
        private String eventTopic;
        @Min(1)
        private int publisherBatchSize;
        @Min(100)
        // Outbox 发布器扫描并发送待投递消息的间隔
        private long publisherIntervalMs;
        @Min(1)
        private long publisherSendingTimeoutSeconds;
        @Min(0)
        private int outboxMaxRetries;
        @Min(1)
        private int sentRetentionDays;
        @Min(1)
        private int cleanupBatchSize;
    }

    @Data
    public static class Task {
        @Min(1)
        private int maxAttempts;
        @Min(3)
        private long executionLeaseSeconds;
        @Min(1000)
        private long recoveryIntervalMs;
        @Min(1)
        private int recoveryBatchSize;
    }

    @Data
    public static class Cache {
        private String keyPrefix = "diary:dev:ai";
        private Integer runningTtlSeconds = 30;
        private Integer terminalTtlHours = 24;
        private Integer idempotencyTtlHours = 24;
        private Integer nullTtlSeconds = 15;
    }

    @Data
    public static class Limit {
        @Min(1)
        private int submitPerUserPerMinute;
        @Min(1)
        private int modelLocalConcurrency;
        @Min(0)
        private long localPermitWaitMs;
    }
}
