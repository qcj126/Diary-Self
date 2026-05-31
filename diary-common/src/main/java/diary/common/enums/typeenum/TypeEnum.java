package diary.common.enums.typeenum;

import lombok.Getter;

import static diary.common.consts.FileTypeConst.DIET_IMAGE;
import static diary.common.consts.FileTypeConst.INGREDIENT_IMAGE;
import static diary.common.consts.FileTypeConst.RECIPE_IMAGE;

@Getter
public enum TypeEnum {
    // 1000---1100    图片的类别
    IMAGE(1000, DIET_IMAGE),
    VIDEO(1001, RECIPE_IMAGE),
    AUDIO(1002, INGREDIENT_IMAGE),
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
