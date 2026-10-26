package diary.file.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import diary.file.entity.po.ImagePO;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageFileService;
import diary.file.utils.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 管理图片的本地副本；本地未命中时才从 OSS 恢复。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageFileServiceImpl implements ImageFileService {
    private final ImageMapper imageMapper;
    private final FileUtil fileUtil;
    private final OSS diaryOssClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Override
    public Path getOrRestore(String originalName) {
        String safeOriginalName = fileUtil.getSafeOriginalName(originalName);
        Path localPath = fileUtil.resolveAppPicture(safeOriginalName);
        if (Files.isRegularFile(localPath)) {
            return localPath;
        }

        ImagePO image = imageMapper.selectImageByOriginalName(safeOriginalName);
        return getOrRestore(image);
    }

    /**
     * 读取指定图片的本地副本；缺失时从 OSS 取回并写入 AppPicture。
     */
    @Override
    public Path getOrRestore(ImagePO image) {
        if (image == null || image.getOriginalName() == null) {
            return null;
        }
        String safeOriginalName = fileUtil.getSafeOriginalName(image.getOriginalName());
        Path localPath = fileUtil.resolveAppPicture(safeOriginalName);
        if (Files.isRegularFile(localPath)) {
            return localPath;
        }
        if (image.getObjectKey() == null) {
            return null;
        }

        try (OSSObject ossObject = diaryOssClient.getObject(bucketName, image.getObjectKey());
             InputStream inputStream = ossObject.getObjectContent()) {
            fileUtil.saveToAppPicture(inputStream, safeOriginalName);
            log.info("从 OSS 恢复图片到本地: {} -> {}", image.getObjectKey(), localPath);
            return localPath;
        } catch (Exception e) {
            log.warn("图片本地未命中且无法从 OSS 恢复: {}", image.getObjectKey(), e);
            return null;
        }
    }

    @Override
    public void deleteLocalFile(Path localPath) {
        try {
            if (Files.deleteIfExists(localPath)) {
                log.info("已删除 OSS 上传失败的本地图片: {}", localPath);
            }
        } catch (IOException e) {
            log.warn("删除 OSS 上传失败的本地图片失败: {}", localPath, e);
        }
    }
}
