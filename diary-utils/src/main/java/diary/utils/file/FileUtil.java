package diary.utils.file;

import diary.common.exception.CustomException;
import org.springframework.stereotype.Component;

import static diary.common.consts.FileTypeConst.DIET_IMAGE;
import static diary.common.consts.FileTypeConst.DIET_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.INGREDIENT_IMAGE;
import static diary.common.consts.FileTypeConst.INGREDIENT_IMAGE_PATH;
import static diary.common.consts.FileTypeConst.RECIPE_IMAGE;
import static diary.common.consts.FileTypeConst.RECIPE_IMAGE_PATH;

@Component
public class FileUtil {
    public String getFileName(String type, String originalFilename) {
        return switch (type) {
            case DIET_IMAGE -> DIET_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case RECIPE_IMAGE -> RECIPE_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            case INGREDIENT_IMAGE -> INGREDIENT_IMAGE_PATH + System.currentTimeMillis() + originalFilename;
            default -> throw new CustomException("未知图片类型");
        };
    }
}
