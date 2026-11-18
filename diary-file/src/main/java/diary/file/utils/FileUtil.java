package diary.file.utils;

import diary.common.exception.ParamIllegalException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static diary.file.consts.FileDirConst.APP_PICTURE_DIR;
import static diary.file.consts.ImageConst.SHA256_PATTERN;

@Component
public class FileUtil {
    /**
     * 将客户端提供的文件名限制为单个文件名，避免路径穿越并保证本地、OSS 使用同一名称。
     */
    public String getSafeOriginalName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String fileName = Path.of(originalFilename).getFileName().toString();
        if (fileName.isBlank() || ".".equals(fileName) || "..".equals(fileName)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        return fileName;
    }

    public File saveToAppPicture(InputStream inputStream, String originalName) throws IOException {
        Path target = resolveAppPicture(originalName);
        Files.createDirectories(APP_PICTURE_DIR);
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toFile();
    }

    public Path resolveAppPicture(String originalName) {
        Path target = APP_PICTURE_DIR.resolve(getSafeOriginalName(originalName)).normalize();
        if (!target.startsWith(APP_PICTURE_DIR)) {
            throw new IllegalArgumentException("文件名不合法");
        }
        return target;
    }

    public static boolean isFileEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    /**
     * 判断sha256是否合法
     */
    public void isSha256Valid(String requestSha256) {
        if (requestSha256 == null || !SHA256_PATTERN.matcher(requestSha256).matches()) {
            throw new ParamIllegalException("sha256必须是64位小写十六进制字符串");
        }
    }
}
