package diary.diaryai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

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
        private String eventTag = "AI_COMPLETED";
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