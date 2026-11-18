package diary.file.impl.thumbnail;

import diary.file.entity.model.GeneratedThumbnail;
import diary.file.entity.model.OssObjectMetadata;
import diary.file.entity.model.OssObjectResource;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.OssOperationEnum;
import diary.file.enums.ThumbnailStatusEnum;
import diary.file.exception.OssStorageException;
import diary.file.exception.PermanentThumbnailProcessingException;
import diary.file.exception.RetryableThumbnailProcessingException;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.mq.event.ImageUploadedEvent;
import diary.file.service.filetransactionalservice.ThumbnailTransactionalService;
import diary.file.service.oss.OssStorageService;
import diary.file.service.thumbnail.ThumbnailImageTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageThumbnailProcessServiceImplTest {
    private static final long IMAGE_ID = 22L;
    private static final long USER_ID = 11L;
    private static final String ORIGINAL_KEY = "images/original/11/2000/22-file.jpg";

    private ImageMapper imageMapper;
    private ThumbnailMapper thumbnailMapper;
    private OssStorageService ossStorageService;
    private ThumbnailImageTransformer transformer;
    private ThumbnailTransactionalService transactionalService;
    private ImageThumbnailProcessServiceImpl service;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        thumbnailMapper = mock(ThumbnailMapper.class);
        ossStorageService = mock(OssStorageService.class);
        transformer = mock(ThumbnailImageTransformer.class);
        transactionalService = mock(ThumbnailTransactionalService.class);
        service = new ImageThumbnailProcessServiceImpl(
                imageMapper, thumbnailMapper, ossStorageService, transformer, transactionalService
        );
    }

    @Test
    void generatesUploadsAndAtomicallyConfirmsThumbnail() {
        ImagePO image = image(ImageStatusEnum.UPLOADED);
        byte[] original = {1, 2, 3};
        byte[] thumbnailBytes = {4, 5, 6, 7};
        GeneratedThumbnail generated = new GeneratedThumbnail(
                thumbnailBytes, "image/jpeg", "jpg", 480, 270, 1600, 900
        );
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);
        when(ossStorageService.openObject(ORIGINAL_KEY)).thenReturn(
                new OssObjectResource(new ByteArrayInputStream(original), original.length, "image/jpeg")
        );
        when(transformer.transform(any(), eq("image/jpeg"))).thenReturn(generated);
        when(ossStorageService.putObject(any(), eq("image/jpeg"), any(), eq(4L))).thenReturn("etag");

        service.process(event());

        ArgumentCaptor<ThumbnailPO> thumbnailCaptor = ArgumentCaptor.forClass(ThumbnailPO.class);
        verify(transactionalService).confirmReady(eq(IMAGE_ID), thumbnailCaptor.capture(), eq(1600), eq(900));
        ThumbnailPO thumbnail = thumbnailCaptor.getValue();
        assertEquals("images/thumbnail/11/22/v1.jpg", thumbnail.getObjectKey());
        assertEquals(ThumbnailStatusEnum.READY.getCode(), thumbnail.getStatus());
        assertEquals(4L, thumbnail.getFileSize());
    }

    @Test
    void existingReadyThumbnailAndOssObjectAreIdempotent() {
        ImagePO image = image(ImageStatusEnum.READY);
        image.setWidth(1600);
        image.setHeight(900);
        ThumbnailPO existing = ThumbnailPO.builder()
                .id(33L).imageId(IMAGE_ID).userId(USER_ID).fileSize(100L)
                .mimeType("image/jpeg").width(480).height(270)
                .objectKey("images/thumbnail/11/22/v1.jpg")
                .status(ThumbnailStatusEnum.READY.getCode()).deleted(0).build();
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);
        when(thumbnailMapper.selectByImageIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(existing);
        when(ossStorageService.objectExists(existing.getObjectKey())).thenReturn(true);
        when(ossStorageService.getObjectMetadata(existing.getObjectKey()))
                .thenReturn(new OssObjectMetadata(100L, "image/jpeg", "etag"));

        service.process(event());

        verify(transactionalService).confirmReady(IMAGE_ID, existing, 1600, 900);
        verify(ossStorageService, never()).openObject(any());
        verify(ossStorageService, never()).putObject(any(), any(), any(), anyLong());
    }

    @Test
    void permanentDecodeFailureIsPersistedBeforeBeingReported() {
        ImagePO image = image(ImageStatusEnum.UPLOADED);
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);
        when(ossStorageService.openObject(ORIGINAL_KEY)).thenReturn(
                new OssObjectResource(new ByteArrayInputStream(new byte[]{1}), 1, "image/jpeg")
        );
        when(transformer.transform(any(), eq("image/jpeg")))
                .thenThrow(new PermanentThumbnailProcessingException("bad image"));

        assertThrows(PermanentThumbnailProcessingException.class, () -> service.process(event()));

        verify(transactionalService).recordPermanentFailure(eq(IMAGE_ID), any(ThumbnailPO.class));
        verify(ossStorageService, never()).putObject(any(), any(), any(), anyLong());
    }

    @Test
    void unsupportedAuthoritativeMimeIsPersistedAsPermanentFailure() {
        ImagePO image = image(ImageStatusEnum.UPLOADED);
        image.setMimeType("image/gif");
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);

        assertThrows(PermanentThumbnailProcessingException.class, () -> service.process(event()));

        verify(transactionalService).recordPermanentFailure(eq(IMAGE_ID), any(ThumbnailPO.class));
        verify(ossStorageService, never()).openObject(any());
    }

    @Test
    void ossReadFailureRemainsRetryableAndDoesNotWriteFailureState() {
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image(ImageStatusEnum.UPLOADED));
        when(ossStorageService.openObject(ORIGINAL_KEY)).thenThrow(
                new OssStorageException(OssOperationEnum.GET_OBJECT.name(), ORIGINAL_KEY, new RuntimeException("network"))
        );

        assertThrows(RetryableThumbnailProcessingException.class, () -> service.process(event()));

        verify(transactionalService, never()).recordPermanentFailure(eq(IMAGE_ID), any());
        verify(transactionalService, never()).confirmReady(eq(IMAGE_ID), any(), eq(0), eq(0));
    }

    @Test
    void historicalEventForNonThumbnailTypeOnlyMarksImageReady() {
        ImagePO image = image(ImageStatusEnum.UPLOADED);
        image.setType(3000);
        when(imageMapper.selectForThumbnailById(IMAGE_ID)).thenReturn(image);

        service.process(event());

        verify(transactionalService).confirmReadyWithoutThumbnail(IMAGE_ID);
        verify(ossStorageService, never()).openObject(any());
        verify(ossStorageService, never()).putObject(any(), any(), any(), anyLong());
    }

    private ImageUploadedEvent event() {
        return ImageUploadedEvent.builder()
                .eventId("event-1")
                .schemaVersion(1)
                .eventType("IMAGE_UPLOADED")
                .imageId(IMAGE_ID)
                .objectKey(ORIGINAL_KEY)
                .originalName("file.jpg")
                .occurredTime(System.currentTimeMillis())
                .build();
    }

    private ImagePO image(ImageStatusEnum status) {
        ImagePO image = new ImagePO();
        image.setId(IMAGE_ID);
        image.setUserId(USER_ID);
        image.setStatus(status.getCode());
        image.setObjectKey(ORIGINAL_KEY);
        image.setOriginalName("file.jpg");
        image.setMimeType("image/jpeg");
        image.setType(2000);
        return image;
    }
}
