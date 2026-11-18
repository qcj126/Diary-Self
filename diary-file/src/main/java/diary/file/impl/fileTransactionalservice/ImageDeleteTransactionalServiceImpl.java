package diary.file.impl.fileTransactionalservice;

import diary.common.exception.NullResultException;
import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.mapper.FileMqOutboxMapper;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.mq.outbox.ImageDeletedOutboxFactory;
import diary.file.service.filetransactionalservice.ImageDeleteTransactionalService;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static diary.file.consts.FileMqProtocolConst.IMAGE_DELETED_EVENT_TYPE;

/** 原子执行图片逻辑删除、缩略图逻辑删除和可靠清理事件落库。 */
@Service
@RequiredArgsConstructor
public class ImageDeleteTransactionalServiceImpl implements ImageDeleteTransactionalService {
    private final ImageMapper imageMapper;
    private final ThumbnailMapper thumbnailMapper;
    private final FileMqOutboxMapper outboxMapper;
    private final ImageDeletedOutboxFactory outboxFactory;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAndRecordCleanupEvent(Long imageId, Long userId) {
        MyUtils.check().notNull(imageId, "imageId").notNull(userId, "userId");
        /*锁定图片行，使两个实例并发删除时串行核验同一唯一删除事件。*/
        ImagePO image = imageMapper.selectAnyByIdAndUserIdForUpdate(imageId, userId);
        if (image == null) {
            throw new NullResultException("图片不存在或无权删除");
        }
        ThumbnailPO thumbnail = thumbnailMapper.selectAnyByImageIdAndUserId(imageId, userId);
        FileMqOutboxPO existing = outboxMapper.selectByImageIdAndEventType(imageId, IMAGE_DELETED_EVENT_TYPE);

        if (!Integer.valueOf(1).equals(image.getDeleted())) {
            if (imageMapper.softDeleteOwnedImage(imageId, userId) != 1) {
                throw new IllegalStateException("图片逻辑删除发生并发冲突, imageId=" + imageId);
            }
            if (thumbnail != null && !Integer.valueOf(1).equals(thumbnail.getDeleted())) {
                thumbnailMapper.softDeleteByImageIdAndUserId(imageId, userId);
            }
        }
        if (existing == null) {
            FileMqOutboxPO created = outboxFactory.create(
                    imageId, image.getObjectKey(), thumbnail == null ? null : thumbnail.getObjectKey()
            );
            if (outboxMapper.insert(created) != 1) {
                throw new IllegalStateException("IMAGE_DELETED Outbox插入失败, imageId=" + imageId);
            }
        } else {
            outboxFactory.validateExisting(
                    existing,
                    imageId,
                    image.getObjectKey(),
                    thumbnail == null ? null : thumbnail.getObjectKey()
            );
        }
    }
}
