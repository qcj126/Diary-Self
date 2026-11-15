package diary.file.enums;

import diary.common.exception.CustomException;
import lombok.Getter;

import static diary.file.consts.FileTypeConst.BEAN_MATCHING_IMAGE;
import static diary.file.consts.FileTypeConst.DAILY_IMAGE;
import static diary.file.consts.FileTypeConst.DIET_IMAGE;
import static diary.file.consts.FileTypeConst.EXERCISE_IMAGE;
import static diary.file.consts.FileTypeConst.GIFT_IMAGE;
import static diary.file.consts.FileTypeConst.INGREDIENT_IMAGE;
import static diary.file.consts.FileTypeConst.RECIPE_IMAGE;
import static diary.file.consts.FileTypeConst.SNACK_IMAGE;
import static diary.file.consts.FileTypeConst.TEA_IMAGE;
import static diary.file.consts.FileTypeConst.TRAVEL_IMAGE;
import static diary.file.consts.FileTypeConst.WALK_IMAGE;

@Getter
public enum ImageTypeEnum {
    /**
     * 图片所属的业务分类。
     *
     * <p>该编码参与 {@code user_id + type + sha256} 唯一约束，不能将其解释为
     * 原图、缩略图或裁剪图等文件形态。</p>
     */
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

    private final int code;
    private final String description;

    ImageTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据业务分类编码获取枚举。
     */
    public static ImageTypeEnum fromCode(Integer code) {
        if (code != null) {
            for (ImageTypeEnum imageTypeEnum : values()) {
                if (imageTypeEnum.code == code) {
                    return imageTypeEnum;
                }
            }
        }
        throw new CustomException("未知图片类型，无效的code: " + code);
    }

    /**
     * 保留旧调用方式，返回业务分类名称。
     */
    public static String getType(Integer code) {
        return fromCode(code).description;
    }

    /**
     * 校验并返回业务分类编码，兼容现有上传链路。
     */
    public static Integer getCode(Integer code) {
        return fromCode(code).code;
    }
}
