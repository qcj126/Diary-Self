package diary.common.enums.goalenum;

import lombok.Getter;

@Getter
public enum GoalCheckinTypeEnum {
    NORMAL(1, "正常打卡"),
    COMPLETE(2, "完成目标"),
    REOPEN(3, "重新开始"),
    MAKE_UP(4, "补打卡");

    private final Integer code;
    private final String name;

    GoalCheckinTypeEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }

    public static GoalCheckinTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GoalCheckinTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static String getNameByCode(Integer code) {
        GoalCheckinTypeEnum value = fromCode(code);
        return value == null ? "未知" : value.name;
    }
}
