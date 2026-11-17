package diary.file.utils;

import diary.common.exception.ParamIllegalException;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

    public File saveToAppPicture(MultipartFile multipartFile, String originalName) throws IOException {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            return saveToAppPicture(inputStream, originalName);
        }
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

    /**
     * 删除文件
     * @param filePath 文件完整路径
     */
    public void deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            // 检查文件是否存在
            if (!file.exists()) log.warn("文件不存在，路径: {}", filePath);
            // 删除文件
            boolean deleted = file.delete();
            if (deleted) {
                log.info("文件删除成功: {}", filePath);
            } else {
                log.error("文件删除失败: {}", filePath);
            }
        } catch (SecurityException e) {
            log.error("文件删除权限异常，路径: {}, 错误: {}", filePath, e.getMessage());
        } catch (Exception e) {
            log.error("文件删除异常，路径: {}, 错误: {}", filePath, e.getMessage());
        }
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
