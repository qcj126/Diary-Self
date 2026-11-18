package diary.file.impl.fileTransactionalservice;

import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.mapper.FileMqOutboxMapper;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.mq.outbox.ImageDeletedOutboxFactory;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageDeleteTransactionalServiceImplTest {
    @Test
    void softDeletesImageAndThumbnailAndRecordsCleanupEvent() {
        ImageMapper imageMapper = mock(ImageMapper.class);
        ThumbnailMapper thumbnailMapper = mock(ThumbnailMapper.class);
        FileMqOutboxMapper outboxMapper = mock(FileMqOutboxMapper.class);
        ImageDeletedOutboxFactory factory = mock(ImageDeletedOutboxFactory.class);
        ImageDeleteTransactionalServiceImpl service = new ImageDeleteTransactionalServiceImpl(
                imageMapper, thumbnailMapper, outboxMapper, factory
        );
        ImagePO image = new ImagePO();
        image.setId(22L);
        image.setUserId(11L);
        image.setDeleted(0);
        image.setObjectKey("images/original/11/22.jpg");
        ThumbnailPO thumbnail = ThumbnailPO.builder().imageId(22L).userId(11L).deleted(0)
                .objectKey("images/thumbnail/11/22/v1.jpg").build();
        FileMqOutboxPO outbox = new FileMqOutboxPO();
        when(imageMapper.selectAnyByIdAndUserIdForUpdate(22L, 11L)).thenReturn(image);
        when(thumbnailMapper.selectAnyByImageIdAndUserId(22L, 11L)).thenReturn(thumbnail);
        when(imageMapper.softDeleteOwnedImage(22L, 11L)).thenReturn(1);
        when(factory.create(22L, image.getObjectKey(), thumbnail.getObjectKey())).thenReturn(outbox);
        when(outboxMapper.insert(outbox)).thenReturn(1);

        service.deleteAndRecordCleanupEvent(22L, 11L);

        verify(imageMapper).softDeleteOwnedImage(22L, 11L);
        verify(thumbnailMapper).softDeleteByImageIdAndUserId(22L, 11L);
        verify(outboxMapper).insert(outbox);
    }
}
