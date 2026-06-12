package diary.common.enums.typeenum;

import diary.common.exception.CustomException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import static diary.common.consts.FileTypeConst.DIET_IMAGE;
import static diary.common.consts.FileTypeConst.INGREDIENT_IMAGE;
import static diary.common.consts.FileTypeConst.RECIPE_IMAGE;

@Getter
public enum TypeEnum {
    // 1000---1100    图片的类别
    IMAGE(1000, DIET_IMAGE),
    VIDEO(2000, RECIPE_IMAGE),
    AUDIO(3000, INGREDIENT_IMAGE),
    ;
    private final Integer code;
    private final String type;

    TypeEnum(Integer code, String type) {
        this.code = code;
        this.type = type;
    }

    public static String getType(Integer code) {
        String type = null;
        for (TypeEnum typeEnum : TypeEnum.values()) {
            if (typeEnum.getCode().equals(code)) {
                type = typeEnum.getType();
            }
        }
        if (type == null) {
            throw new CustomException("未知图片类型，无效的code: " + code);
        }
        return type;
    }

    public static Integer getCode(Integer code) {
        for (TypeEnum typeEnum : TypeEnum.values()) {
            if (typeEnum.getCode().equals(code)) {
                return typeEnum.getCode();
            }
        }
        throw new CustomException("未知图片类型，无效的code: " + code);
    }
}
