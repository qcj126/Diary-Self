package diary.common.enums.typeenum;

import diary.common.exception.CustomException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import static diary.common.consts.FileTypeConst.BEAN_MATCHING_IMAGE;
import static diary.common.consts.FileTypeConst.DAILY_IMAGE;
import static diary.common.consts.FileTypeConst.DIET_IMAGE;
import static diary.common.consts.FileTypeConst.EXERCISE_IMAGE;
import static diary.common.consts.FileTypeConst.GIFT_IMAGE;
import static diary.common.consts.FileTypeConst.INGREDIENT_IMAGE;
import static diary.common.consts.FileTypeConst.RECIPE_IMAGE;
import static diary.common.consts.FileTypeConst.SNACK_IMAGE;
import static diary.common.consts.FileTypeConst.TEA_IMAGE;
import static diary.common.consts.FileTypeConst.TRAVEL_IMAGE;
import static diary.common.consts.FileTypeConst.WALK_IMAGE;

@Getter
public enum TypeEnum {
    // 1000---1100    图片的类别
    DIET(1000, DIET_IMAGE),
    RECIPE(2000, RECIPE_IMAGE),
    INGREDIENT(3000, INGREDIENT_IMAGE),
    BEAN_MATCHING(4000, BEAN_MATCHING_IMAGE),
    GIFT(5000, GIFT_IMAGE),
    SNACK(6000, SNACK_IMAGE),
    TEA(7000, TEA_IMAGE),
    TRAVEL(8000, TRAVEL_IMAGE),
    DAILY(9000, DAILY_IMAGE),
    EXERCISE(10000, EXERCISE_IMAGE),
    WALK(11000, WALK_IMAGE),

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
