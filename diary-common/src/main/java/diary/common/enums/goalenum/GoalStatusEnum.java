package diary.common.enums.goalenum;

import lombok.Getter;

@Getter
public enum GoalStatusEnum {
    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    PAUSED(2, "暂停"),
    COMPLETED(3, "已完成"),
    ABANDONED(4, "已放弃");

    private final Integer code;
    private final String name;

    GoalStatusEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
}
