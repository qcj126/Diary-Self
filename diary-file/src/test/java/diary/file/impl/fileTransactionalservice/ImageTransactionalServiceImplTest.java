package diary.file.impl.fileTransactionalservice;

import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.mapper.FileMqOutboxMapper;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.mapper.ImageUploadChunkMapper;
import diary.file.mq.outbox.ImageUploadedOutboxFactory;
import diary.file.state.ImageStatusTransitionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_EVENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageTransactionalServiceImplTest {
    private static final String UPLOAD_ID = "upload-id";
    private static final long USER_ID = 11L;
    private static final long IMAGE_ID = 22L;

    private ImageMapper imageMapper;
    private ImageUploadSessionMapper sessionMapper;
    private FileMqOutboxMapper outboxMapper;
    private ImageUploadedOutboxFactory outboxFactory;
    private ImageTransactionalServiceImpl service;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        sessionMapper = mock(ImageUploadSessionMapper.class);
        outboxMapper = mock(FileMqOutboxMapper.class);
        outboxFactory = mock(ImageUploadedOutboxFactory.class);
        service = new ImageTransactionalServiceImpl(
                imageMapper,
                sessionMapper,
                mock(ImageUploadChunkMapper.class),
                new ImageStatusTransitionPolicy(),
                outboxMapper,
                outboxFactory
        );
    }

    @Test
    void completesSessionImageAndOutboxInOneOrderedOperation() {
        ImageUploadSessionPO session = session(ImageUploadSessionStatusEnum.COMPLETING, 7);
        ImagePO image = image(ImageStatusEnum.UPLOADING);
        FileMqOutboxPO outbox = new FileMqOutboxPO();
        when(sessionMapper.selectByUploadIdAndUserIdForUpdate(UPLOAD_ID, USER_ID)).thenReturn(session);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(image);
        when(imageMapper.updateStatusIfCurrent(
                IMAGE_ID, ImageStatusEnum.UPLOADING.getCode(), ImageStatusEnum.UPLOADED.getCode()
        )).thenReturn(1);
        when(sessionMapper.updateStatusIfVersion(
                UPLOAD_ID, USER_ID,
                ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETED.getCode(), 7
        )).thenReturn(1);
        when(outboxFactory.create(image)).thenReturn(outbox);
        when(outboxMapper.insert(outbox)).thenReturn(1);

        service.completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);

        InOrder order = inOrder(imageMapper, sessionMapper, outboxMapper);
        order.verify(imageMapper).updateStatusIfCurrent(
                IMAGE_ID, ImageStatusEnum.UPLOADING.getCode(), ImageStatusEnum.UPLOADED.getCode()
        );
        order.verify(sessionMapper).updateStatusIfVersion(
                UPLOAD_ID, USER_ID,
                ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETED.getCode(), 7
        );
        order.verify(outboxMapper).selectByImageIdAndEventType(IMAGE_ID, IMAGE_UPLOADED_EVENT_TYPE);
        order.verify(outboxMapper).insert(outbox);
    }

    @Test
    void completedSessionRepairsMissingOutboxWithoutRepeatingStateUpdates() {
        ImageUploadSessionPO session = session(ImageUploadSessionStatusEnum.COMPLETED, 8);
        ImagePO image = image(ImageStatusEnum.UPLOADED);
        FileMqOutboxPO outbox = new FileMqOutboxPO();
        when(sessionMapper.selectByUploadIdAndUserIdForUpdate(UPLOAD_ID, USER_ID)).thenReturn(session);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(image);
        when(outboxFactory.create(image)).thenReturn(outbox);
        when(outboxMapper.insert(outbox)).thenReturn(1);

        service.completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);

        verify(imageMapper, never()).updateStatusIfCurrent(
                IMAGE_ID, ImageStatusEnum.UPLOADING.getCode(), ImageStatusEnum.UPLOADED.getCode()
        );
        verify(sessionMapper, never()).updateStatusIfVersion(
                UPLOAD_ID, USER_ID,
                ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETED.getCode(), 8
        );
        verify(outboxMapper).insert(outbox);
    }

    @Test
    void completedSessionValidatesExistingOutbox() {
        ImageUploadSessionPO session = session(ImageUploadSessionStatusEnum.COMPLETED, 8);
        ImagePO image = image(ImageStatusEnum.READY);
        FileMqOutboxPO existing = new FileMqOutboxPO();
        when(sessionMapper.selectByUploadIdAndUserIdForUpdate(UPLOAD_ID, USER_ID)).thenReturn(session);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(image);
        when(outboxMapper.selectByImageIdAndEventType(IMAGE_ID, IMAGE_UPLOADED_EVENT_TYPE))
                .thenReturn(existing);

        service.completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);

        verify(outboxFactory).validateExisting(existing, image);
        verify(outboxMapper, never()).insert(existing);
    }

    @Test
    void outboxInsertFailureIsNotSwallowed() {
        ImageUploadSessionPO session = session(ImageUploadSessionStatusEnum.COMPLETING, 7);
        ImagePO image = image(ImageStatusEnum.UPLOADING);
        FileMqOutboxPO outbox = new FileMqOutboxPO();
        when(sessionMapper.selectByUploadIdAndUserIdForUpdate(UPLOAD_ID, USER_ID)).thenReturn(session);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(image);
        when(imageMapper.updateStatusIfCurrent(IMAGE_ID, 1210, 1220)).thenReturn(1);
        when(sessionMapper.updateStatusIfVersion(UPLOAD_ID, USER_ID, 2, 3, 7)).thenReturn(1);
        when(outboxFactory.create(image)).thenReturn(outbox);
        when(outboxMapper.insert(outbox)).thenReturn(0);

        assertThrows(IllegalStateException.class,
                () -> service.completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID));
    }

    @Test
    void completesNonThumbnailImageDirectlyAsReadyWithoutOutbox() {
        ImageUploadSessionPO session = session(ImageUploadSessionStatusEnum.COMPLETING, 7);
        ImagePO image = image(ImageStatusEnum.UPLOADING);
        image.setType(3000);
        when(sessionMapper.selectByUploadIdAndUserIdForUpdate(UPLOAD_ID, USER_ID)).thenReturn(session);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(image);
        when(imageMapper.updateStatusIfCurrent(
                IMAGE_ID, ImageStatusEnum.UPLOADING.getCode(), ImageStatusEnum.READY.getCode()
        )).thenReturn(1);
        when(sessionMapper.updateStatusIfVersion(UPLOAD_ID, USER_ID, 2, 3, 7)).thenReturn(1);

        service.completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);

        verify(imageMapper).updateStatusIfCurrent(IMAGE_ID, 1210, 1000);
        verify(outboxMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(outboxFactory, never()).create(org.mockito.ArgumentMatchers.any());
    }

    private ImageUploadSessionPO session(ImageUploadSessionStatusEnum status, int version) {
        ImageUploadSessionPO session = new ImageUploadSessionPO();
        session.setUploadId(UPLOAD_ID);
        session.setImageId(IMAGE_ID);
        session.setUserId(USER_ID);
        session.setStatus(status.getCode());
        session.setVersion(version);
        return session;
    }

    private ImagePO image(ImageStatusEnum status) {
        ImagePO image = new ImagePO();
        image.setId(IMAGE_ID);
        image.setUserId(USER_ID);
        image.setStatus(status.getCode());
        image.setObjectKey("images/original/11/2000/22-file.jpg");
        image.setOriginalName("file.jpg");
        image.setType(2000);
        return image;
    }
}
