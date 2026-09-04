//package diary.diarylove.properties;
//
//import jakarta.validation.Valid;
//import jakarta.validation.constraints.Min;
//import lombok.Data;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.stereotype.Component;
//import org.springframework.validation.annotation.Validated;
//
//@Data
//@Component
//@Validated
//@ConfigurationProperties(prefix = "diary.love-record")
//public class LoveRecordProperties {
//    @Valid
//    private Rocketmq rocketmq = new Rocketmq();
//    @Valid
//    private Task task = new Task();
//    @Valid
//    private Cache cache = new Cache();
//    @Valid
//    private Limit limit = new Limit();
//
//    @Data
//    public static class Rocketmq {
//        private String taskTopic = "diary-love-record-task";
//        private String taskTag = "LOVE_RECORD";
//        private String taskConsumerGroup = "diary-love-record-worker";
//        private String eventTopic = "diary-love-record-event";
//        /*
//         * 改前：完成与失败事件共用 eventTag=AI_COMPLETED，按 AI_FAILED 过滤的消费者永远收不到失败事件。
//         * 改后：终态事件分别使用独立 Tag，Tag 与 eventType 保持一致。
//         */
//        private String completedTag = "LOVE_RECORD_COMPLETED";
//        private String failedTag = "LOVE_RECORD_FAILED";
//        @Min(1)
//        private int publisherBatchSize = 20;
//        @Min(100)
//        private long publisherIntervalMs = 1000;
//        @Min(1)
//        private long publisherSendingTimeoutSeconds = 60;
//        @Min(0)
//        private int outboxMaxRetries = 10;
//        @Min(1)
//        private int sentRetentionDays = 7;
//        @Min(1)
//        private int cleanupBatchSize = 500;
//    }
//
//    @Data
//    public static class Task {
//        @Min(1)
//        private int maxAttempts = 3;
//        @Min(3)
//        private long executionLeaseSeconds = 330;
//        @Min(1000)
//        private long recoveryIntervalMs = 30000;
//        @Min(1)
//        private int recoveryBatchSize = 50;
//        @Min(1)
//        private long waitingRecoverySeconds = 600;
//        @Min(1)
//        private int waitingMaxRecoveryMessages = 3;
//    }
//
//    @Data
//    public static class Cache {
//        private long runningTtlSeconds = 30;
//        private long terminalTtlHours = 24;
//        private long idempotencyTtlHours = 24;
//        private long nullTtlSeconds = 15;
//        private String keyPrefix = "diary:dev:love-record";
//    }
//
//    @Data
//    public static class Limit {
//        @Min(1)
//        private int submitPerUserPerMinute = 10;
//        @Min(1)
//        private int modelLocalConcurrency = 2;
//        @Min(0)
//        private long localPermitWaitMs = 1000;
//    }
//}
