package diary.file.impl.fileTransactionalservice;

import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ThumbnailStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.state.ImageStatusTransitionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThumbnailTransactionalServiceImplTest {
    private static final long IMAGE_ID = 22L;
    private static final long USER_ID = 11L;

    private ImageMapper imageMapper;
    private ThumbnailMapper thumbnailMapper;
    private ThumbnailTransactionalServiceImpl service;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        thumbnailMapper = mock(ThumbnailMapper.class);
        service = new ThumbnailTransactionalServiceImpl(
                imageMapper, thumbnailMapper, new ImageStatusTransitionPolicy()
        );
    }

    @Test
    void insertsThumbnailAndMarksUploadedImageReady() {
        ImagePO image = image(ImageStatusEnum.UPLOADED);
        ThumbnailPO thumbnail = thumbnail(ThumbnailStatusEnum.READY);
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);
        when(thumbnailMapper.insert(thumbnail)).thenReturn(1);
        when(imageMapper.updateDimensionsIfMissing(IMAGE_ID, 1600, 900)).thenReturn(1);
        when(imageMapper.updateStatusIfCurrent(IMAGE_ID, 1220, 1000)).thenReturn(1);

        service.confirmReady(IMAGE_ID, thumbnail, 1600, 900);

        verify(thumbnailMapper).insert(thumbnail);
        verify(imageMapper).updateStatusIfCurrent(IMAGE_ID, 1220, 1000);
    }

    @Test
    void repeatedReadyConfirmationIsIdempotent() {
        ImagePO image = image(ImageStatusEnum.READY);
        image.setWidth(1600);
        image.setHeight(900);
        ThumbnailPO thumbnail = thumbnail(ThumbnailStatusEnum.READY);
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);
        when(thumbnailMapper.selectByImageIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(thumbnail);

        service.confirmReady(IMAGE_ID, thumbnail, 1600, 900);

        verify(thumbnailMapper, never()).insert(thumbnail);
        verify(imageMapper, never()).updateStatusIfCurrent(IMAGE_ID, 1220, 1000);
    }

    @Test
    void repairsReadyMetadataAfterStableObjectKeyWasReuploaded() {
        ImagePO image = image(ImageStatusEnum.READY);
        image.setWidth(1600);
        image.setHeight(900);
        ThumbnailPO existing = thumbnail(ThumbnailStatusEnum.READY);
        existing.setFileSize(900L);
        ThumbnailPO generated = thumbnail(ThumbnailStatusEnum.READY);
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);
        when(thumbnailMapper.selectByImageIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(existing);
        when(thumbnailMapper.updateIfStatus(generated, ThumbnailStatusEnum.READY.getCode())).thenReturn(1);

        service.confirmReady(IMAGE_ID, generated, 1600, 900);

        verify(thumbnailMapper).updateIfStatus(generated, ThumbnailStatusEnum.READY.getCode());
        verify(imageMapper, never()).updateStatusIfCurrent(IMAGE_ID, 1220, 1000);
    }

    @Test
    void permanentFailureMarksUploadedImageFailed() {
        ImagePO image = image(ImageStatusEnum.UPLOADED);
        ThumbnailPO failed = thumbnail(ThumbnailStatusEnum.FAILED);
        failed.setFileSize(0L);
        failed.setWidth(0);
        failed.setHeight(0);
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);
        when(thumbnailMapper.insert(failed)).thenReturn(1);
        when(imageMapper.updateStatusIfCurrent(IMAGE_ID, 1220, 1100)).thenReturn(1);

        service.recordPermanentFailure(IMAGE_ID, failed);

        verify(thumbnailMapper).insert(failed);
        verify(imageMapper).updateStatusIfCurrent(IMAGE_ID, 1220, 1100);
    }

    private ImagePO image(ImageStatusEnum status) {
        ImagePO image = new ImagePO();
        image.setId(IMAGE_ID);
        image.setUserId(USER_ID);
        image.setStatus(status.getCode());
        return image;
    }

    private ThumbnailPO thumbnail(ThumbnailStatusEnum status) {
        return ThumbnailPO.builder()
                .id(33L)
                .imageId(IMAGE_ID)
                .userId(USER_ID)
                .fileSize(1024L)
                .mimeType("image/jpeg")
                .width(480)
                .height(270)
                .objectKey("images/thumbnail/11/22/v1.jpg")
                .status(status.getCode())
                .deleted(0)
                .build();
    }
}
