package diary.file.impl;

import diary.utils.commonutil.MyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Component
public class IconFileSupport {
    private static final Path ICON_DIR = Paths.get("E:\\Diary-Front\\icon").toAbsolutePath().normalize();

    public SavedIconFile saveIconFile(MultipartFile file) {
        try {
            Files.createDirectories(ICON_DIR);
            String filename = buildStorageFilename(file.getOriginalFilename());
            Path targetPath = ICON_DIR.resolve(filename).normalize();
            if (!targetPath.startsWith(ICON_DIR)) {
                throw new IllegalArgumentException("图标文件名非法");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new SavedIconFile(targetPath.toString());
        } catch (IOException e) {
            throw new RuntimeException("保存图标文件失败", e);
        }
    }

    public String buildStorageFilename(String originalFilename) {
        String filename = MyUtils.isEmpty(originalFilename) ? "icon" : Paths.get(originalFilename).getFileName().toString();
        String sanitizedFilename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String extension = "";
        int dotIndex = sanitizedFilename.lastIndexOf(".");
        if (dotIndex >= 0) {
            extension = sanitizedFilename.substring(dotIndex);
            sanitizedFilename = sanitizedFilename.substring(0, dotIndex);
        }
        if (MyUtils.isEmpty(sanitizedFilename)) {
            sanitizedFilename = "icon";
        }
        return sanitizedFilename + "_" + UUID.randomUUID() + extension;
    }

    public Integer resolveIconPixel(MultipartFile file, Integer iconPixel) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image != null) {
                return Math.max(image.getWidth(), image.getHeight());
            }
        } catch (IOException e) {
            log.warn("读取图标像素失败: {}", file.getOriginalFilename(), e);
        }
        MyUtils.check().notNull(iconPixel, "图标像素大小");
        return iconPixel;
    }

    public Integer getIconType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String lowerFilename = filename == null ? "" : filename.toLowerCase();
        String contentType = file.getContentType();
        if (lowerFilename.endsWith(".svg") || "image/svg+xml".equalsIgnoreCase(contentType)) {
            return 2;
        }
        if (lowerFilename.endsWith(".png") || "image/png".equalsIgnoreCase(contentType)) {
            return 1;
        }
        return 3;
    }

    public Integer toIntFileSize(long size) {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("图标文件过大");
        }
        return (int) size;
    }

    public void deleteFileQuietly(String iconPath) {
        if (MyUtils.isEmpty(iconPath)) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(iconPath));
        } catch (IOException e) {
            log.warn("删除图标文件失败: {}", iconPath, e);
        }
    }

    public record SavedIconFile(String path) {
    }
}
