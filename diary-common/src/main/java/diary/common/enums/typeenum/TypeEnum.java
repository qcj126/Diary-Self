package diary.common.enums.typeenum;

import lombok.Getter;

@Getter
public enum TypeEnum {
    // 1000---1100    图片的类别
    IMAGE(1000, "饮食图片"),
    VIDEO(1001, "食谱图片"),
    AUDIO(1002, "食材图片"),
    ;
    private final Integer code;
    private final String type;

    TypeEnum(Integer code, String type) {
        this.code = code;
        this.type = type;
    }

    public static String getType(Integer code) {
        for (TypeEnum typeEnum : TypeEnum.values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum.getType();
            }
        }
        return null;
    }
}
