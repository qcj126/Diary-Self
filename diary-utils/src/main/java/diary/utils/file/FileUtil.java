package diary.utils.file;

import diary.common.exception.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * 将上传的文件复制到临时文件，为异步上传文件设计
     *
     * @param files 文件列表
     * @return 临时文件列表
     */
    public List<File> copyToTempFiles(List<MultipartFile> files) {
        List<File> tempFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                Path tempFile = Files.createTempFile("diary-image-upload-", ".tmp");
                file.transferTo(tempFile);
                tempFiles.add(tempFile.toFile());
            } catch (IOException e) {
                tempFiles.forEach(File::delete);
                throw new RuntimeException("复制上传临时文件失败", e);
            }
        }
        return tempFiles;
    }
}
