package diary.file.service;

import diary.file.entity.model.OssObjectResource;

/** 读取当前用户已经就绪的原图。 */
public interface OriginalImageService {
    OssObjectResource openByImageId(Long imageId, Long userId);

    OssObjectResource openByOriginalName(String originalName, Long userId);
}
