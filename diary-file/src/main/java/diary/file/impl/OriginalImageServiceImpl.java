package diary.file.impl;

import diary.file.entity.model.OssObjectResource;
import diary.file.entity.po.ImagePO;
import diary.file.enums.ImageStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.service.OriginalImageService;
import diary.file.service.oss.OssStorageService;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OriginalImageServiceImpl implements OriginalImageService {
    private final ImageMapper imageMapper;
    private final OssStorageService ossStorageService;

    @Override
    public OssObjectResource openByImageId(Long imageId, Long userId) {
        MyUtils.check().notNull(imageId, "imageId").notNull(userId, "userId");
        return openReadyImage(imageMapper.selectForUploadByIdAndUserId(imageId, userId));
    }

    @Override
    public OssObjectResource openByOriginalName(String originalName, Long userId) {
        MyUtils.check().notEmpty(originalName, "originalName").notNull(userId, "userId");
        return openReadyImage(imageMapper.selectReadyByOriginalNameAndUserId(originalName, userId));
    }

    private OssObjectResource openReadyImage(ImagePO image) {
        if (image == null
                || !Objects.equals(image.getStatus(), ImageStatusEnum.READY.getCode())
                || image.getObjectKey() == null
                || image.getObjectKey().isBlank()) {
            return null;
        }
        return ossStorageService.openObject(image.getObjectKey());
    }
}
