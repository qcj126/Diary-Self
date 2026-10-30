package diary.diaryai.enums.aienum;

import lombok.Getter;

@Getter
public enum AiTaskErrorCodeEnum {
    PERMANENT_ERROR("永久性错误"),
    RETRYABLE_ERROR("可重试错误"),
    RETRY_EXHAUSTED("重试次数耗尽"),
    SNAPSHOT_INVALID("快照数据无效"),
    SUBMIT_RATE_LIMITED("提交已被限流"),
    MODEL_CAPACITY_EXHAUSTED("模型全局并发配额已满"),
    REDIS_UNAVAILABLE("Redis 模型并发控制不可用"),
    OUTBOX_SEND_FAILED("outbox发送失败");
    private final String displayName;
    AiTaskErrorCodeEnum(String displayName) {
        this.displayName = displayName;
    }
}
