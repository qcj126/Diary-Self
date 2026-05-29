package diary.common.enums.dietenum;

import lombok.Getter;

@Getter
public enum MealTypeEnum {

    BREAKFAST(10, "早餐"),
    MORNING_SNACK(15, "早加餐"),
    LUNCH(20, "午餐"),
    AFTERNOON_SNACK(25, "午加餐"),
    DINNER(30, "晚餐"),
    NIGHT_SNACK(35, "夜宵");

    private final Byte code;
    private final String name;

    MealTypeEnum(Integer code, String name) {
        this.code = code.byteValue();
        this.name = name;
    }

    public static String getNameByCode(Byte code) {
        if (code == null) return null;
        for (MealTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type.name;
            }
        }
        return "未知";
    }
}
