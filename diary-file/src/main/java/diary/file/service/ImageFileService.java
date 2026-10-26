package diary.file.service;

import diary.file.entity.po.ImagePO;

import java.nio.file.Path;

/**
 * 图片本地副本服务。
 */
public interface ImageFileService {
    Path getOrRestore(String originalName);

    Path getOrRestore(ImagePO image);

    void deleteLocalFile(Path localPath);
}
