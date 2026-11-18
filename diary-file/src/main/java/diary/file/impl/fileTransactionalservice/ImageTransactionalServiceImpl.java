package diary.file.impl.fileTransactionalservice;

import diary.common.exception.ConflictException;
import diary.common.exception.ParamIllegalException;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTypeEnum;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.mapper.FileMqOutboxMapper;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.mapper.ImageUploadChunkMapper;
import diary.file.mq.outbox.ImageUploadedOutboxFactory;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.state.ImageStatusTransitionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_EVENT_TYPE;

@Service
@RequiredArgsConstructor
public class ImageTransactionalServiceImpl implements ImageTransactionalService {
    private final ImageMapper imageMapper;
    private final ImageUploadSessionMapper sessionMapper;
    private final ImageUploadChunkMapper chunkMapper;
    private final ImageStatusTransitionPolicy transitionPolicy;
    private final FileMqOutboxMapper outboxMapper;
    private final ImageUploadedOutboxFactory outboxFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initSessionAndImage(ImagePO imagePO, ImageUploadSessionPO sessionPO) {
        if (imagePO == null || sessionPO == null) {
            throw new ParamIllegalException("图片数据或上传会话数据为空");
        }
        int imageCnt = imageMapper.singleAddImageToDb(imagePO);
        int sessionCnt = sessionMapper.insert(sessionPO);
        if (imageCnt != 1 || sessionCnt != 1) {
            throw new IllegalStateException (
                    "图片数据或会话数据插入失败，图片插入数：" + imageCnt + ", 会话插入数：" + sessionCnt
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeMultipartUpload(String uploadId, Long userId, Long imageId) {
        /*
         * 锁定上传会话，串行化同一图片的“完成上传”和“补写缺失Outbox”流程。
         * 多实例并发仍由数据库行锁、状态/版本CAS和唯一索引共同保证。
         */
        ImageUploadSessionPO session = sessionMapper.selectByUploadIdAndUserIdForUpdate(uploadId, userId);
        ImagePO image = imageMapper.selectForUploadByIdAndUserId(imageId, userId);

        if (session == null || image == null || !imageId.equals(session.getImageId())) {
            throw new ConflictException("上传会话与图片记录不一致");
        }

        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());
        boolean thumbnailRequired = ImageTypeEnum.fromCode(image.getType()).isThumbnailRequired();

        if (Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.COMPLETED.getCode())) {
            if (thumbnailRequired && (imageStatus == ImageStatusEnum.UPLOADED || imageStatus == ImageStatusEnum.READY)) {
                ensureImageUploadedOutbox(image);
                return;
            }
            if (!thumbnailRequired && imageStatus == ImageStatusEnum.UPLOADED) {
                markReadyWithoutThumbnail(image);
                return;
            }
            if (!thumbnailRequired && imageStatus == ImageStatusEnum.READY) return;
            throw new ConflictException("会话已完成，但图片状态不一致");
        }

        if (!Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.COMPLETING.getCode())) {
            throw new ConflictException("上传会话不处于合并中状态");
        }

        if (imageStatus == ImageStatusEnum.UPLOADING) {
            ImageStatusEnum targetStatus = thumbnailRequired ? ImageStatusEnum.UPLOADED : ImageStatusEnum.READY;
            if (!transitionPolicy.canTransition(imageStatus, targetStatus)) {
                throw new ConflictException("图片状态不允许迁移到" + targetStatus.name());
            }

            int imageUpdated = imageMapper.updateStatusIfCurrent(imageId, imageStatus.getCode(), targetStatus.getCode());

            if (imageUpdated != 1) {
                throw new ConflictException("图片状态并发更新冲突");
            }
        } else if (imageStatus != ImageStatusEnum.UPLOADED && imageStatus != ImageStatusEnum.READY) {
            throw new ConflictException("图片当前状态不能完成上传：" + imageStatus.name());
        }

        int sessionUpdated = sessionMapper.updateStatusIfVersion(
                uploadId, userId, ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETED.getCode(), session.getVersion()
        );

        if (sessionUpdated != 1) {
            throw new ConflictException("上传会话最终状态并发更新冲突");
        }

        /*
         * Outbox插入和两项状态更新属于同一个本地事务。这里不能发送RocketMQ，
         * 任意数据库操作失败都会让session、image和Outbox一起回滚。
         */
        if (thumbnailRequired) {
            ensureImageUploadedOutbox(image);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failMultipartUpload(String uploadId, Long userId, Long imageId) {
        ImageUploadSessionPO session = sessionMapper.selectByUploadIdAndUserId(uploadId, userId);
        ImagePO image = imageMapper.selectForUploadByIdAndUserId(imageId, userId);

        if (session == null || image == null || !imageId.equals(session.getImageId())) {
            throw new ConflictException("上传会话与图片记录不一致");
        }

        ImageStatusEnum imageStatus = ImageStatusEnum.fromCode(image.getStatus());

        if (Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.ABORTED.getCode())) {
            if (imageStatus == ImageStatusEnum.UPLOAD_FAILED) {
                return;
            }
            throw new ConflictException("会话已中止，但图片状态不一致");
        }

        if (!Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.COMPLETING.getCode())) {
            throw new ConflictException("上传会话不处于合并中状态");
        }

        if (imageStatus != ImageStatusEnum.UPLOAD_FAILED) {
            if (!transitionPolicy.canTransition(imageStatus, ImageStatusEnum.UPLOAD_FAILED)) {
                throw new ConflictException("图片状态不允许迁移到UPLOAD_FAILED");
            }

            int imageUpdated = imageMapper.updateStatusIfCurrent(imageId, imageStatus.getCode(), ImageStatusEnum.UPLOAD_FAILED.getCode());

            if (imageUpdated != 1) {
                throw new ConflictException("图片失败状态并发更新冲突");
            }
        }

        int sessionUpdated = sessionMapper.updateStatusIfVersion(
                uploadId, userId, ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                ImageUploadSessionStatusEnum.ABORTED.getCode(), session.getVersion()
        );

        if (sessionUpdated != 1) {
            throw new ConflictException("上传会话失败状态并发更新冲突");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimExpiredSession(ImageUploadSessionPO candidate) {
        if (candidate == null || candidate.getUploadId() == null || candidate.getUserId() == null
                || candidate.getStatus() == null || candidate.getVersion() == null) {
            throw new ParamIllegalException("过期上传会话候选数据不完整");
        }
        if (Objects.equals(candidate.getStatus(), ImageUploadSessionStatusEnum.EXPIRED.getCode())) {
            return true;
        }
        if (!Objects.equals(candidate.getStatus(), ImageUploadSessionStatusEnum.INITIATING.getCode())
                && !Objects.equals(candidate.getStatus(), ImageUploadSessionStatusEnum.UPLOADING.getCode())) {
            return false;
        }
        return sessionMapper.updateStatusIfVersion(
                candidate.getUploadId(), candidate.getUserId(), candidate.getStatus(),
                ImageUploadSessionStatusEnum.EXPIRED.getCode(), candidate.getVersion()
        ) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredSession(String uploadId, Long userId, Long imageId) {
        ImageUploadSessionPO session = sessionMapper.selectByUploadIdAndUserIdForUpdate(uploadId, userId);
        if (session == null) {
            return;
        }
        if (!Objects.equals(session.getImageId(), imageId)
                || !Objects.equals(session.getStatus(), ImageUploadSessionStatusEnum.EXPIRED.getCode())) {
            throw new ConflictException("上传会话不允许执行过期清理");
        }

        ImagePO image = imageMapper.selectAnyByIdAndUserId(imageId, userId);
        if (image != null && !Objects.equals(image.getDeleted(), 1)) {
            int deleted = imageMapper.softDeleteAbandonedUpload(imageId, userId);
            if (deleted != 1) {
                throw new ConflictException("图片已进入完成态，拒绝清理过期上传会话");
            }
        }
        chunkMapper.deleteByUploadId(uploadId);
        if (sessionMapper.deleteByUploadIdAndUserId(uploadId, userId) != 1) {
            throw new IllegalStateException("过期上传会话删除失败, uploadId=" + uploadId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupOrphanChunks(int limit) {
        if (limit <= 0) {
            throw new ParamIllegalException("孤儿分片清理上限必须大于0");
        }
        return chunkMapper.deleteOrphans(limit);
    }

    private void ensureImageUploadedOutbox(ImagePO image) {
        FileMqOutboxPO existing = outboxMapper.selectByImageIdAndEventType(
                image.getId(), IMAGE_UPLOADED_EVENT_TYPE
        );
        if (existing != null) {
            outboxFactory.validateExisting(existing, image);
            return;
        }

        FileMqOutboxPO created = outboxFactory.create(image);
        int inserted = outboxMapper.insert(created);
        if (inserted != 1) {
            throw new IllegalStateException("IMAGE_UPLOADED Outbox插入失败, imageId=" + image.getId());
        }
    }

    private void markReadyWithoutThumbnail(ImagePO image) {
        if (!transitionPolicy.canTransition(ImageStatusEnum.UPLOADED, ImageStatusEnum.READY)
                || imageMapper.updateStatusIfCurrent(
                image.getId(), ImageStatusEnum.UPLOADED.getCode(), ImageStatusEnum.READY.getCode()
        ) != 1) {
            throw new ConflictException("无需缩略图的图片READY状态恢复失败");
        }
    }
}
