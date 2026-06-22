package diary.utils.file;

import diary.common.exception.CustomException;
import org.springframework.stereotype.Component;

import static diary.common.consts.FileTypeConst.BEAN_MATCHING_IMAGE;
import static diary.common.consts.FileTypeConst.BEAN_MATCHING_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.DAILY_IMAGE;
import static diary.common.consts.FileTypeConst.DAILY_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.DIET_IMAGE;
import static diary.common.consts.FileTypeConst.DIET_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.EXERCISE_IMAGE;
import static diary.common.consts.FileTypeConst.EXERCISE_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.GIFT_IMAGE;
import static diary.common.consts.FileTypeConst.GIFT_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.INGREDIENT_IMAGE;
import static diary.common.consts.FileTypeConst.INGREDIENT_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.RECIPE_IMAGE;
import static diary.common.consts.FileTypeConst.RECIPE_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.SNACK_IMAGE;
import static diary.common.consts.FileTypeConst.SNACK_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.TEA_IMAGE;
import static diary.common.consts.FileTypeConst.TEA_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.TRAVEL_IMAGE;
import static diary.common.consts.FileTypeConst.TRAVEL_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.WALK_IMAGE;
import static diary.common.consts.FileTypeConst.WALK_IMAGE_PATH;

@Component
public class FileUtil {
    public String getFileName(String type, String originalFilename) {
        return switch (type) {
            case DIET_IMAGE -> DIET_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case RECIPE_IMAGE -> RECIPE_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case INGREDIENT_IMAGE -> INGREDIENT_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case BEAN_MATCHING_IMAGE -> BEAN_MATCHING_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case GIFT_IMAGE -> GIFT_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case SNACK_IMAGE -> SNACK_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case TEA_IMAGE -> TEA_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case TRAVEL_IMAGE -> TRAVEL_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case DAILY_IMAGE -> DAILY_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case EXERCISE_IMAGE -> EXERCISE_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case WALK_IMAGE -> WALK_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            default -> throw new CustomException("未知图片类型");
        };
    }
}
