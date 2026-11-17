package diary.file.impl.uploadserviceImpl;

import diary.common.exception.ParamIllegalException;
import diary.file.entity.dto.ImageUploadInitReqDto;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadChunkPO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.entity.vo.ImageUploadConfigVO;
import diary.file.entity.vo.ImageUploadInitVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadChunkMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.property.ImageUploadProperties;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.service.oss.OssStorageService;
import diary.file.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageUploadInitServiceImplTest {

    private static final long USER_ID = 101L;
    private static final int IMAGE_TYPE = 2_000;
    private static final int CHUNK_SIZE = 262_144;
    private static final int MAX_IMAGE_SIZE = 104_857_600;
    private static final String SHA256 = "b".repeat(64);

    private FileUtil fileUtil;
    private ImageMapper imageMapper;
    private ImageUploadSessionMapper sessionMapper;
    private ImageUploadChunkMapper chunkMapper;
    private OssStorageService ossStorageService;
    private ImageTransactionalService transactionalService;
    private ImageUploadInitServiceImpl service;

    @BeforeEach
    void setUp() {
        ImageUploadProperties properties = new ImageUploadProperties();
        properties.setChunkSize(CHUNK_SIZE);
        properties.setMaxImageSize(MAX_IMAGE_SIZE);
        properties.setSessionTtlHours(24);

        fileUtil = mock(FileUtil.class);
        imageMapper = mock(ImageMapper.class);
        sessionMapper = mock(ImageUploadSessionMapper.class);
        chunkMapper = mock(ImageUploadChunkMapper.class);
        ossStorageService = mock(OssStorageService.class);
        transactionalService = mock(ImageTransactionalService.class);
        service = new ImageUploadInitServiceImpl(
                properties,
                fileUtil,
                imageMapper,
                sessionMapper,
                chunkMapper,
                ossStorageService,
                transactionalService
        );
    }

    @Test
    void initializeReturnsInstantUploadForReadyImage() {
        ImagePO ready = image(7001L, ImageStatusEnum.READY);
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");
        when(imageMapper.selectReadyByUserIdAndTypeAndSha256(USER_ID, IMAGE_TYPE, SHA256)).thenReturn(ready);

        ImageUploadInitVO result = service.initialize(validRequest(), USER_ID);

        assertEquals(7001L, result.getImageId());
        assertTrue(result.isInstantUpload());
        assertNull(result.getUploadId());
        assertEquals(List.of(), result.getUploadedChunkIndexes());
        verifyNoInteractions(ossStorageService, transactionalService, sessionMapper, chunkMapper);
    }

    @Test
    void initializeResumesOnlyUnexpiredUploadingSessionAndReturnsChunkIndexes() {
        ImagePO uploading = image(7002L, ImageStatusEnum.UPLOADING);
        ImageUploadSessionPO session = ImageUploadSessionPO.builder()
                .uploadId("business-upload-id")
                .ossUploadId("internal-oss-upload-id")
                .status(ImageUploadSessionStatusEnum.UPLOADING.getCode())
                .chunkSize(CHUNK_SIZE)
                .totalChunks(2)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");
        when(imageMapper.selectByUserIdAndTypeAndSha256(USER_ID, IMAGE_TYPE, SHA256)).thenReturn(uploading);
        when(sessionMapper.selectByImageIdAndUserId(7002L, USER_ID)).thenReturn(session);
        when(chunkMapper.selectByUploadIdOrderByPartNumber("business-upload-id")).thenReturn(List.of(
                ImageUploadChunkPO.builder().chunkIndex(0).partNumber(1).build(),
                ImageUploadChunkPO.builder().chunkIndex(1).partNumber(2).build()
        ));

        ImageUploadInitVO result = service.initialize(validRequest(), USER_ID);

        assertFalse(result.isInstantUpload());
        assertEquals("business-upload-id", result.getUploadId());
        assertEquals(List.of(0, 1), result.getUploadedChunkIndexes());
        assertEquals(CHUNK_SIZE, result.getChunkSize());
        assertEquals(2, result.getTotalChunks());
        verifyNoInteractions(ossStorageService, transactionalService);
    }

    @Test
    void initializeCreatesOssUploadThenPersistsTrustedSessionValues() {
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");
        when(ossStorageService.initiateMultipartUpload(anyString(), eq("image/jpeg")))
                .thenReturn("internal-oss-upload-id");

        ImageUploadInitVO result = service.initialize(validRequest(), USER_ID);

        ArgumentCaptor<String> objectKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(ossStorageService).initiateMultipartUpload(objectKeyCaptor.capture(), eq("image/jpeg"));
        ArgumentCaptor<ImagePO> imageCaptor = ArgumentCaptor.forClass(ImagePO.class);
        ArgumentCaptor<ImageUploadSessionPO> sessionCaptor = ArgumentCaptor.forClass(ImageUploadSessionPO.class);
        verify(transactionalService).initSessionAndImage(imageCaptor.capture(), sessionCaptor.capture());

        ImagePO image = imageCaptor.getValue();
        ImageUploadSessionPO session = sessionCaptor.getValue();
        String objectKey = objectKeyCaptor.getValue();
        assertTrue(objectKey.contains(String.valueOf(image.getId())));
        assertTrue(objectKey.contains(SHA256));
        assertEquals(objectKey, image.getObjectKey());
        assertEquals(objectKey, session.getObjectKey());
        assertEquals("internal-oss-upload-id", session.getOssUploadId());
        assertEquals(CHUNK_SIZE, session.getChunkSize());
        assertEquals(2, session.getTotalChunks());
        assertNotNull(result.getUploadId());
        assertFalse(result.getUploadId().equals(session.getOssUploadId()));
        assertEquals(result.getUploadId(), session.getUploadId());
        assertEquals(List.of(), result.getUploadedChunkIndexes());
    }

    @Test
    void initializeAbortsOssUploadAndRethrowsOriginalDatabaseFailure() {
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");
        when(ossStorageService.initiateMultipartUpload(anyString(), eq("image/jpeg")))
                .thenReturn("internal-oss-upload-id");
        RuntimeException databaseFailure = new RuntimeException("database failed");
        doThrow(databaseFailure).when(transactionalService).initSessionAndImage(any(), any());

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.initialize(validRequest(), USER_ID));

        assertSame(databaseFailure, thrown);
        ArgumentCaptor<String> objectKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(ossStorageService).initiateMultipartUpload(objectKeyCaptor.capture(), eq("image/jpeg"));
        verify(ossStorageService).abortMultipartUpload(objectKeyCaptor.getValue(), "internal-oss-upload-id");
    }

    @Test
    void initializeKeepsDatabaseFailureWhenAbortAlsoFails() {
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");
        when(ossStorageService.initiateMultipartUpload(anyString(), eq("image/jpeg")))
                .thenReturn("internal-oss-upload-id");
        RuntimeException databaseFailure = new RuntimeException("database failed");
        RuntimeException abortFailure = new RuntimeException("abort failed");
        doThrow(databaseFailure).when(transactionalService).initSessionAndImage(any(), any());
        doThrow(abortFailure).when(ossStorageService).abortMultipartUpload(anyString(), eq("internal-oss-upload-id"));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.initialize(validRequest(), USER_ID));

        assertSame(databaseFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(abortFailure, thrown.getSuppressed()[0]);
    }

    @Test
    void initializeRecoversConcurrentDuplicateUsingWinningUpload() {
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");
        when(ossStorageService.initiateMultipartUpload(anyString(), eq("image/jpeg")))
                .thenReturn("losing-oss-upload-id");
        ImagePO winner = image(8001L, ImageStatusEnum.READY);
        when(imageMapper.selectReadyByUserIdAndTypeAndSha256(USER_ID, IMAGE_TYPE, SHA256))
                .thenReturn(null, winner);
        doThrow(new DuplicateKeyException("concurrent insert"))
                .when(transactionalService).initSessionAndImage(any(), any());

        ImageUploadInitVO result = service.initialize(validRequest(), USER_ID);

        assertTrue(result.isInstantUpload());
        assertEquals(8001L, result.getImageId());
        verify(ossStorageService).abortMultipartUpload(anyString(), eq("losing-oss-upload-id"));
    }

    @Test
    void initializeRejectsMimeTypeThatDoesNotMatchExtensionBeforeOssCall() {
        ImageUploadInitReqDto request = validRequest();
        request.setMimeType("image/png");
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");

        ParamIllegalException exception = assertThrows(ParamIllegalException.class,
                () -> service.initialize(request, USER_ID));

        assertEquals("文件扩展名与mimeType不匹配", exception.getMessage());
        verifyNoInteractions(ossStorageService, transactionalService);
    }

    @Test
    void initializeRejectsBlankOssUploadIdBeforeDatabaseCall() {
        when(fileUtil.getSafeOriginalName("photo.jpg")).thenReturn("photo.jpg");
        when(ossStorageService.initiateMultipartUpload(anyString(), eq("image/jpeg"))).thenReturn("   ");

        assertThrows(ParamIllegalException.class, () -> service.initialize(validRequest(), USER_ID));

        verify(transactionalService, never()).initSessionAndImage(any(), any());
        verify(ossStorageService, never()).abortMultipartUpload(anyString(), anyString());
    }

    @Test
    void getUploadConfigReturnsServerLimits() {
        ImageUploadConfigVO config = service.getUploadConfig();

        assertEquals(CHUNK_SIZE, config.getChunkSize());
        assertEquals(MAX_IMAGE_SIZE, config.getMaxImageSize());
        assertEquals(List.of("image/png", "image/jpeg"), config.getAllowedMimeTypes());
        assertEquals(List.of("png", "jpg", "jpeg"), config.getAllowedExtensions());
    }

    private ImageUploadInitReqDto validRequest() {
        ImageUploadInitReqDto request = new ImageUploadInitReqDto();
        request.setOriginalName("photo.jpg");
        request.setFileSize(300_000L);
        request.setMimeType("image/jpeg");
        request.setSha256(SHA256);
        request.setChunkSize(CHUNK_SIZE);
        request.setTotalChunks(2);
        request.setImageTypeCode(IMAGE_TYPE);
        return request;
    }

    private ImagePO image(long imageId, ImageStatusEnum status) {
        ImagePO image = new ImagePO();
        image.setId(imageId);
        image.setStatus(status.getCode());
        return image;
    }
}
