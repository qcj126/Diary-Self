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
    private Redis redis = new Redis();
    @Valid
    private Limit limit = new Limit();

    @Data
    public static class Rocketmq {
        private String taskTopic = "diary-ai-task";
        private String taskTag = "QWEN_PLUS_NUTRIENT";
        private String taskConsumerGroup = "diary-ai-qwen-plus-worker-v2";
        private String eventTopic = "diary-ai-event";
        private String completedTag = "AI_COMPLETED";
        private String failedTag = "AI_FAILED";
        @Min(1)
        private int publisherBatchSize = 20;
        @Min(100)
        private long publisherIntervalMs = 1000;
        @Min(1)
        private long publisherSendingTimeoutSeconds = 60;
        @Min(0)
        private int outboxMaxRetries = 10;
        @Min(1)
        private int sentRetentionDays = 7;
        @Min(1)
        private int cleanupBatchSize = 500;
    }

    @Data
    public static class Task {
        @Min(1)
        private int maxAttempts = 3;
        @Min(3)
        private long executionLeaseSeconds = 330;
        @Min(1000)
        private long recoveryIntervalMs = 30000;
        @Min(1)
        private int recoveryBatchSize = 50;
    }

    @Data
    public static class Redis {
        private String keyPrefix = "diary:dev:ai";
    }

    @Data
    public static class Limit {
        @Min(1)
        private int submitPerUserPerMinute = 10;
        @Min(1)
        private int modelLocalConcurrency = 2;
        @Min(0)
        private long localPermitWaitMs = 1000;
    }
}
