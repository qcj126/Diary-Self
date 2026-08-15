package diary.common.enums.outbox;

import lombok.Getter;

@Getter
public enum OutboxStatusEnum {
    NEW("新建任务消息"),
    SENDING("消息发送中"),
    RETRY_WAIT("重试等待"),
    SENT("消息已发送"),
    DEAD("消息已死亡");

    OutboxStatusEnum(String displayName) {
        this.displayName = displayName;
    }

    private final String displayName;
}
