package diary.file.impl.thumbnail;

import diary.file.entity.model.GeneratedThumbnail;
import diary.file.entity.model.OssObjectMetadata;
import diary.file.entity.model.OssObjectResource;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTypeEnum;
import diary.file.enums.ThumbnailStatusEnum;
import diary.file.exception.OssStorageException;
import diary.file.exception.PermanentThumbnailProcessingException;
import diary.file.exception.RetryableThumbnailProcessingException;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.mq.event.ImageUploadedEvent;
import diary.file.service.filetransactionalservice.ThumbnailTransactionalService;
import diary.file.service.oss.OssStorageService;
import diary.file.service.thumbnail.ImageThumbnailProcessService;
import diary.file.service.thumbnail.ThumbnailImageTransformer;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * 缩略图处理编排。
 *
 * <p>所有OSS调用都在数据库事务外执行。稳定objectKey允许重复消费者覆盖同一对象；最终一致性由
 * thumbnail.image_id唯一约束、缩略图元数据核验和图片状态CAS共同保证。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageThumbnailProcessServiceImpl implements ImageThumbnailProcessService {
    private static final String JPEG_MIME = "image/jpeg";
    private static final String PNG_MIME = "image/png";
    private static final String THUMBNAIL_KEY_VERSION = "v1";

    private final ImageMapper imageMapper;
    private final ThumbnailMapper thumbnailMapper;
    private final OssStorageService ossStorageService;
    private final ThumbnailImageTransformer transformer;
    private final ThumbnailTransactionalService transactionalService;

    @Override
    public void process(ImageUploadedEvent event) {
        validateEvent(event);
        ImagePO image = imageMapper.selectForThumbnailById(event.getImageId());
        if (image == null) {
            throw new PermanentThumbnailProcessingException("图片不存在或已删除, imageId=" + event.getImageId());
        }
        validateAuthoritativeImage(event, image);

        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());
        if (!ImageTypeEnum.fromCode(image.getType()).isThumbnailRequired()) {
            if (imageStatus == ImageStatusEnum.UPLOADED) {
                transactionalService.confirmReadyWithoutThumbnail(image.getId());
            } else if (imageStatus != ImageStatusEnum.READY) {
                throw new RetryableThumbnailProcessingException(
                        "无需缩略图的图片尚未进入可完成状态, imageId=" + image.getId()
                );
            }
            log.info("图片类型无需生成缩略图，事件幂等完成, imageId={}, type={}", image.getId(), image.getType());
            return;
        }
        if (imageStatus == ImageStatusEnum.UPLOAD_FAILED) {
            log.info("图片已处于失败终态，忽略缩略图事件, imageId={}", image.getId());
            return;
        }
        if (imageStatus != ImageStatusEnum.UPLOADED && imageStatus != ImageStatusEnum.READY) {
            throw new RetryableThumbnailProcessingException(
                    "图片尚未进入可生成缩略图的状态, imageId=" + image.getId() + ", status=" + imageStatus.name()
            );
        }

        String normalizedMimeType;
        try {
            normalizedMimeType = normalizeMimeType(image.getMimeType());
        } catch (PermanentThumbnailProcessingException exception) {
            /*数据库中的权威MIME本身非法时也必须落失败终态，不能让毒消息无限重试。*/
            recordPermanentFailure(
                    image,
                    thumbnailObjectKey(image, "invalid"),
                    "application/octet-stream",
                    exception
            );
            throw exception;
        }
        String extension = PNG_MIME.equals(normalizedMimeType) ? "png" : "jpg";
        String thumbnailObjectKey = thumbnailObjectKey(image, extension);
        ThumbnailPO existing = thumbnailMapper.selectByImageIdAndUserId(image.getId(), image.getUserId());
        if (handleExistingReady(image, existing, thumbnailObjectKey)) {
            return;
        }
        if (existing != null && Objects.equals(existing.getStatus(), ThumbnailStatusEnum.FAILED.getCode())) {
            if (imageStatus == ImageStatusEnum.UPLOADED) {
                /*兼容“失败缩略图已提交、图片终态尚未提交”的历史/异常数据。*/
                transactionalService.recordPermanentFailure(image.getId(), existing);
            }
            log.info("缩略图已经记录永久失败，忽略重复事件, imageId={}", image.getId());
            return;
        }

        try {
            GeneratedThumbnail generated = generate(image, normalizedMimeType);
            /*缩略图字节数受最大宽高推导出的上限约束，可以安全地以内存流上传小对象。*/
            try (ByteArrayInputStream input = new ByteArrayInputStream(generated.bytes())) {
                ossStorageService.putObject(
                        thumbnailObjectKey,
                        generated.contentType(),
                        input,
                        generated.bytes().length
                );
            }

            ThumbnailPO readyThumbnail = readyThumbnail(image, generated, thumbnailObjectKey);
            transactionalService.confirmReady(
                    image.getId(), readyThumbnail, generated.sourceWidth(), generated.sourceHeight()
            );
            log.info(
                    "缩略图处理完成, imageId={}, objectKey={}, width={}, height={}, bytes={}",
                    image.getId(), thumbnailObjectKey, generated.width(), generated.height(), generated.bytes().length
            );
        } catch (PermanentThumbnailProcessingException exception) {
            recordPermanentFailure(image, thumbnailObjectKey, normalizedMimeType, exception);
            throw exception;
        } catch (OssStorageException exception) {
            throw new RetryableThumbnailProcessingException(
                    "缩略图OSS操作失败, imageId=" + image.getId(), exception
            );
        } catch (RetryableThumbnailProcessingException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RetryableThumbnailProcessingException(
                    "缩略图数据库提交或并发校验失败, imageId=" + image.getId(), exception
            );
        } catch (IOException exception) {
            throw new RetryableThumbnailProcessingException(
                    "关闭缩略图内存流失败, imageId=" + image.getId(), exception
            );
        }
    }

    private GeneratedThumbnail generate(ImagePO image, String expectedMimeType) {
        try (OssObjectResource resource = ossStorageService.openObject(image.getObjectKey())) {
            String ossMimeType = normalizeMimeType(resource.getContentType());
            if (!expectedMimeType.equals(ossMimeType)) {
                throw new PermanentThumbnailProcessingException(
                        "OSS对象MIME与数据库不一致, expected=" + expectedMimeType + ", actual=" + ossMimeType
                );
            }
            return transformer.transform(resource.getInputStream(), expectedMimeType);
        } catch (PermanentThumbnailProcessingException exception) {
            throw exception;
        } catch (OssStorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new RetryableThumbnailProcessingException(
                    "关闭或读取OSS原图流失败, imageId=" + image.getId(), exception
            );
        }
    }

    private boolean handleExistingReady(ImagePO image, ThumbnailPO existing, String expectedObjectKey) {
        if (existing == null || !Objects.equals(existing.getStatus(), ThumbnailStatusEnum.READY.getCode())) {
            return false;
        }
        validateExistingIdentity(image, existing, expectedObjectKey);
        if (!ossStorageService.objectExists(existing.getObjectKey())) {
            log.warn("READY缩略图数据库记录存在但OSS对象缺失，将重新生成, imageId={}, objectKey={}",
                    image.getId(), existing.getObjectKey());
            return false;
        }
        OssObjectMetadata metadata = ossStorageService.getObjectMetadata(existing.getObjectKey());
        if (metadata.contentLength() != existing.getFileSize()
                || !sameMimeType(metadata.contentType(), existing.getMimeType())) {
            log.warn(
                    "READY缩略图的OSS元数据与数据库不一致，将覆盖稳定objectKey并修复记录, imageId={}, objectKey={}",
                    image.getId(), existing.getObjectKey()
            );
            return false;
        }
        if (image.getWidth() != null && image.getHeight() != null) {
            transactionalService.confirmReady(image.getId(), existing, image.getWidth(), image.getHeight());
        } else if (ImageStatusEnum.fromCode(image.getStatus()) == ImageStatusEnum.READY) {
            /*兼容历史READY数据：缩略图已完整且OSS存在时，不为补宽高而重复下载原图。*/
            return true;
        } else {
            return false;
        }
        log.info("重复缩略图事件幂等完成, imageId={}, objectKey={}", image.getId(), existing.getObjectKey());
        return true;
    }

    private void recordPermanentFailure(
            ImagePO image,
            String thumbnailObjectKey,
            String mimeType,
            PermanentThumbnailProcessingException sourceException
    ) {
        ThumbnailPO failed = ThumbnailPO.builder()
                .id(MyUtils.getPrimaryKey())
                .imageId(image.getId())
                .userId(image.getUserId())
                .fileSize(0L)
                .mimeType(mimeType)
                .width(0)
                .height(0)
                .objectKey(thumbnailObjectKey)
                .status(ThumbnailStatusEnum.FAILED.getCode())
                .deleted(0)
                .build();
        try {
            transactionalService.recordPermanentFailure(image.getId(), failed);
            log.error("缩略图永久处理失败，已记录失败终态, imageId={}, reason={}",
                    image.getId(), sourceException.getMessage());
        } catch (RuntimeException databaseException) {
            sourceException.addSuppressed(databaseException);
            throw new RetryableThumbnailProcessingException(
                    "缩略图永久错误已确认，但失败状态落库失败, imageId=" + image.getId(), sourceException
            );
        }
    }

    private ThumbnailPO readyThumbnail(ImagePO image, GeneratedThumbnail generated, String objectKey) {
        return ThumbnailPO.builder()
                .id(MyUtils.getPrimaryKey())
                .imageId(image.getId())
                .userId(image.getUserId())
                .fileSize((long) generated.bytes().length)
                .mimeType(generated.contentType())
                .width(generated.width())
                .height(generated.height())
                .objectKey(objectKey)
                .status(ThumbnailStatusEnum.READY.getCode())
                .deleted(0)
                .build();
    }

    private String thumbnailObjectKey(ImagePO image, String extension) {
        return "images/thumbnail/" + image.getUserId() + "/" + image.getId()
                + "/" + THUMBNAIL_KEY_VERSION + "." + extension;
    }

    private void validateEvent(ImageUploadedEvent event) {
        MyUtils.check()
                .notNull(event, "event")
                .notNull(event.getImageId(), "event.imageId")
                .notEmpty(event.getObjectKey(), "event.objectKey");
    }

    private void validateAuthoritativeImage(ImageUploadedEvent event, ImagePO image) {
        if (!Objects.equals(event.getObjectKey(), image.getObjectKey())) {
            throw new PermanentThumbnailProcessingException(
                    "事件objectKey与数据库不一致, imageId=" + image.getId()
            );
        }
        if (event.getOriginalName() != null
                && !Objects.equals(event.getOriginalName(), image.getOriginalName())) {
            throw new PermanentThumbnailProcessingException(
                    "事件originalName与数据库不一致, imageId=" + image.getId()
            );
        }
    }

    private void validateExistingIdentity(ImagePO image, ThumbnailPO thumbnail, String expectedObjectKey) {
        if (!Objects.equals(thumbnail.getImageId(), image.getId())
                || !Objects.equals(thumbnail.getUserId(), image.getUserId())
                || !Objects.equals(thumbnail.getObjectKey(), expectedObjectKey)) {
            throw new PermanentThumbnailProcessingException(
                    "已有READY缩略图记录身份冲突, imageId=" + image.getId()
            );
        }
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new PermanentThumbnailProcessingException("图片MIME为空");
        }
        String normalized = mimeType.trim().toLowerCase(Locale.ROOT);
        if ("image/jpg".equals(normalized)) {
            return JPEG_MIME;
        }
        if (!JPEG_MIME.equals(normalized) && !PNG_MIME.equals(normalized)) {
            throw new PermanentThumbnailProcessingException("不支持的图片MIME：" + mimeType);
        }
        return normalized;
    }

    private boolean sameMimeType(String first, String second) {
        if (first == null || second == null) {
            return false;
        }
        return first.trim().equalsIgnoreCase(second.trim());
    }
}
