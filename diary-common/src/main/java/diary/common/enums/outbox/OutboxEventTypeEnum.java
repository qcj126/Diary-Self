package diary.common.enums.outbox;

import lombok.Getter;

@Getter
public enum OutboxEventTypeEnum {
    AI_TASK_CREATED("任务已创建"),
    AI_COMPLETED("任务已完成"),
    AI_FAILED("任务失败");

    private final String displayName;

    OutboxEventTypeEnum(String displayName) {
        this.displayName = displayName;
    }
}
