package diary.common.enums.loveenum;

import lombok.Getter;

@Getter
public enum LoveRecordStatusEnum {
    PENDING("待处理"),
    QUEUED("已排队"),
    RUNNING("运行中"),
    RETRY_WAIT("重试等待"),
    SUCCESS("成功"),
    FAILED("失败"),
    CANCELLED("已取消"),
    DEAD_LETTER("死信");

    private final String displayName;

    LoveRecordStatusEnum(String displayName) {
        this.displayName = displayName;
    }
    public boolean isTerminal() {
        return this == SUCCESS
                || this == FAILED
                || this == CANCELLED
                || this == DEAD_LETTER;
    }
}
