package diary.common.enums.aienum;

import lombok.Getter;

@Getter
public enum AiTaskErrorCodeEnum {
    PERMANENT_ERROR("永久性错误"),
    RETRYABLE_ERROR("可重试错误"),
    RETRY_EXHAUSTED("重试次数耗尽"),
    SNAPSHOT_INVALID("快照数据无效"),
    SUBMIT_RATE_LIMITED("提交已被限流"),
    OUTBOX_SEND_FAILED("outbox发送失败");
    private final String displayName;
    AiTaskErrorCodeEnum(String displayName) {
        this.displayName = displayName;
    }
}
