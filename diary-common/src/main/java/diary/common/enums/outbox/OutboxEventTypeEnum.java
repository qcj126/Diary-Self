package diary.common.enums.outbox;

import lombok.Getter;

@Getter
public enum OutboxEventTypeEnum {
    AI_TASK_CREATED("AI任务已创建"),
    AI_TASK_RETRY("AI任务已重试"),
    AI_COMPLETED("AI任务已完成"),
    AI_FAILED("AI任务失败"),
    LOVE_RECORD_TASK_CREATED("爱情记录任务已创建"),
    LOVE_RECORD_TASK_RETRY("爱情记录任务已重试"),
    LOVE_RECORD_COMPLETED("爱情记录已完成"),
    LOVE_RECORD_FAILED("爱情记录失败");

    private final String displayName;

    OutboxEventTypeEnum(String displayName) {
        this.displayName = displayName;
    }
}
