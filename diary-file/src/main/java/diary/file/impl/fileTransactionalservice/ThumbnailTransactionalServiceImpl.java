package diary.file.impl.fileTransactionalservice;

import diary.common.exception.ConflictException;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ThumbnailStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.service.filetransactionalservice.ThumbnailTransactionalService;
import diary.file.state.ImageStatusTransitionPolicy;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 原子提交缩略图记录和图片最终状态。
 *
 * <p>OSS上传必须在调用本服务前完成。数据库失败时OSS对象保留在稳定objectKey，消息重试可覆盖并恢复。</p>
 */
@Service
@RequiredArgsConstructor
public class ThumbnailTransactionalServiceImpl implements ThumbnailTransactionalService {
    private final ImageMapper imageMapper;
    private final ThumbnailMapper thumbnailMapper;
    private final ImageStatusTransitionPolicy transitionPolicy;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReady(Long imageId, ThumbnailPO thumbnail, int sourceWidth, int sourceHeight) {
        validateRequest(imageId, thumbnail);
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            throw new ConflictException("原图宽高必须大于0");
        }

        ImagePO image = requireImage(imageId);
        validateImageOwner(image, thumbnail);
        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());
        if (imageStatus != ImageStatusEnum.UPLOADED && imageStatus != ImageStatusEnum.READY) {
            throw new ConflictException("图片状态不允许确认缩略图READY：" + imageStatus.name());
        }

        ThumbnailPO existing = thumbnailMapper.selectByImageIdAndUserId(imageId, image.getUserId());
        persistReadyThumbnail(existing, thumbnail);
        validateOrFillSourceDimensions(image, sourceWidth, sourceHeight);

        if (imageStatus == ImageStatusEnum.UPLOADED) {
            if (!transitionPolicy.canTransition(imageStatus, ImageStatusEnum.READY)) {
                throw new ConflictException("图片状态不允许迁移到READY");
            }
            int updated = imageMapper.updateStatusIfCurrent(
                    imageId, ImageStatusEnum.UPLOADED.getCode(), ImageStatusEnum.READY.getCode()
            );
            if (updated != 1) {
                ImagePO latest = requireImage(imageId);
                if (ImageStatusEnum.fromCode(latest.getStatus()) != ImageStatusEnum.READY) {
                    throw new ConflictException("图片READY状态并发更新冲突");
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordPermanentFailure(Long imageId, ThumbnailPO failedThumbnail) {
        validateRequest(imageId, failedThumbnail);
        if (!Objects.equals(failedThumbnail.getStatus(), ThumbnailStatusEnum.FAILED.getCode())) {
            throw new ConflictException("永久失败记录必须使用FAILED状态");
        }

        ImagePO image = requireImage(imageId);
        validateImageOwner(image, failedThumbnail);
        ThumbnailPO existing = thumbnailMapper.selectByImageIdAndUserId(imageId, image.getUserId());
        persistFailedThumbnail(existing, failedThumbnail);

        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());
        if (imageStatus == ImageStatusEnum.UPLOADED) {
            if (!transitionPolicy.canTransition(imageStatus, ImageStatusEnum.UPLOAD_FAILED)) {
                throw new ConflictException("图片状态不允许迁移到UPLOAD_FAILED");
            }
            int updated = imageMapper.updateStatusIfCurrent(
                    imageId, ImageStatusEnum.UPLOADED.getCode(), ImageStatusEnum.UPLOAD_FAILED.getCode()
            );
            if (updated != 1) {
                ImagePO latest = requireImage(imageId);
                if (ImageStatusEnum.fromCode(latest.getStatus()) != ImageStatusEnum.UPLOAD_FAILED) {
                    throw new ConflictException("图片失败状态并发更新冲突");
                }
            }
        } else if (imageStatus != ImageStatusEnum.READY && imageStatus != ImageStatusEnum.UPLOAD_FAILED) {
            throw new ConflictException("图片状态不允许记录缩略图永久失败：" + imageStatus.name());
        }
        /*历史READY图片保持可访问，仅记录缩略图失败；新链路的UPLOADED图片会进入UPLOAD_FAILED。*/
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReadyWithoutThumbnail(Long imageId) {
        ImagePO image = requireImage(imageId);
        ImageStatusEnum status = ImageStatusEnum.fromCode(image.getStatus());
        if (status == ImageStatusEnum.READY) return;
        if (status != ImageStatusEnum.UPLOADED
                || !transitionPolicy.canTransition(status, ImageStatusEnum.READY)
                || imageMapper.updateStatusIfCurrent(
                imageId, ImageStatusEnum.UPLOADED.getCode(), ImageStatusEnum.READY.getCode()
        ) != 1) {
            throw new ConflictException("无需缩略图的图片READY状态迁移失败");
        }
    }

    private void persistReadyThumbnail(ThumbnailPO existing, ThumbnailPO expected) {
        if (!Objects.equals(expected.getStatus(), ThumbnailStatusEnum.READY.getCode())) {
            throw new ConflictException("确认缩略图必须使用READY状态");
        }
        if (existing == null) {
            if (thumbnailMapper.insert(expected) != 1) {
                throw new IllegalStateException("缩略图记录插入失败, imageId=" + expected.getImageId());
            }
            return;
        }

        validateThumbnailIdentity(existing, expected);
        ThumbnailStatusEnum status = ThumbnailStatusEnum.fromCode(existing.getStatus());
        if (status == ThumbnailStatusEnum.READY) {
            if (!readyMetadataMatches(existing, expected)
                    && thumbnailMapper.updateIfStatus(expected, ThumbnailStatusEnum.READY.getCode()) != 1) {
                throw new ConflictException("READY缩略图元数据修复发生并发冲突");
            }
            return;
        }
        int updated = thumbnailMapper.updateIfStatus(expected, status.getCode());
        if (updated != 1) {
            throw new ConflictException("缩略图READY状态并发更新冲突");
        }
    }

    private void persistFailedThumbnail(ThumbnailPO existing, ThumbnailPO failed) {
        if (existing == null) {
            if (thumbnailMapper.insert(failed) != 1) {
                throw new IllegalStateException("缩略图失败记录插入失败, imageId=" + failed.getImageId());
            }
            return;
        }

        validateThumbnailIdentity(existing, failed);
        ThumbnailStatusEnum status = ThumbnailStatusEnum.fromCode(existing.getStatus());
        if (status == ThumbnailStatusEnum.FAILED) {
            return;
        }
        if (thumbnailMapper.updateIfStatus(failed, status.getCode()) != 1) {
            throw new ConflictException("缩略图FAILED状态并发更新冲突");
        }
    }

    private void validateOrFillSourceDimensions(ImagePO image, int width, int height) {
        if (image.getWidth() != null && image.getWidth() != width) {
            throw new ConflictException("原图宽度与数据库不一致");
        }
        if (image.getHeight() != null && image.getHeight() != height) {
            throw new ConflictException("原图高度与数据库不一致");
        }
        if ((image.getWidth() == null || image.getHeight() == null)
                && imageMapper.updateDimensionsIfMissing(image.getId(), width, height) != 1) {
            throw new ConflictException("原图宽高回写失败");
        }
    }

    private ImagePO requireImage(Long imageId) {
        ImagePO image = imageMapper.selectForThumbnailById(imageId);
        if (image == null) {
            throw new ConflictException("图片不存在或已删除, imageId=" + imageId);
        }
        return image;
    }

    private void validateRequest(Long imageId, ThumbnailPO thumbnail) {
        MyUtils.check()
                .notNull(imageId, "imageId")
                .notNull(thumbnail, "thumbnail")
                .notNull(thumbnail.getId(), "thumbnailId")
                .notNull(thumbnail.getImageId(), "thumbnail.imageId")
                .notNull(thumbnail.getUserId(), "thumbnail.userId")
                .notEmpty(thumbnail.getObjectKey(), "thumbnail.objectKey")
                .notNull(thumbnail.getStatus(), "thumbnail.status");
        if (!Objects.equals(imageId, thumbnail.getImageId())) {
            throw new ConflictException("缩略图与图片ID不一致");
        }
    }

    private void validateImageOwner(ImagePO image, ThumbnailPO thumbnail) {
        if (!Objects.equals(image.getUserId(), thumbnail.getUserId())) {
            throw new ConflictException("缩略图用户与图片用户不一致");
        }
    }

    private void validateThumbnailIdentity(ThumbnailPO existing, ThumbnailPO expected) {
        if (!Objects.equals(existing.getImageId(), expected.getImageId())
                || !Objects.equals(existing.getUserId(), expected.getUserId())
                || !Objects.equals(existing.getObjectKey(), expected.getObjectKey())) {
            throw new ConflictException("已有缩略图记录与预期身份不一致");
        }
    }

    private boolean readyMetadataMatches(ThumbnailPO existing, ThumbnailPO expected) {
        return Objects.equals(existing.getFileSize(), expected.getFileSize())
                && Objects.equals(existing.getMimeType(), expected.getMimeType())
                && Objects.equals(existing.getWidth(), expected.getWidth())
                && Objects.equals(existing.getHeight(), expected.getHeight());
    }
}
