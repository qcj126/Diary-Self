package diary.file.impl.uploadserviceImpl;

import diary.common.exception.ConflictException;
import diary.common.exception.ParamIllegalException;
import diary.file.entity.dto.ImageUploadCompleteReqDto;
import diary.file.entity.model.OssCompleteResult;
import diary.file.entity.model.OssObjectResource;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ImageUploadChunkPO;
import diary.file.entity.po.ImageUploadSessionPO;
import diary.file.entity.vo.ImageUploadCompleteVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.enums.ImageUploadSessionStatusEnum;
import diary.file.enums.OssCompleteState;
import diary.file.exception.OssStorageException;
import diary.file.exception.OssObjectSizeMismatchException;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ImageUploadChunkMapper;
import diary.file.mapper.ImageUploadSessionMapper;
import diary.file.service.ImageStatusService;
import diary.file.service.filetransactionalservice.ImageTransactionalService;
import diary.file.service.oss.OssStorageService;
import diary.file.utils.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageUploadCompleteServiceImplTest {

    private static final String UPLOAD_ID = "business-upload-id";
    private static final String OSS_UPLOAD_ID = "oss-upload-id";
    private static final String OBJECT_KEY = "images/original/11/2000/image.jpg";
    private static final long USER_ID = 11L;
    private static final long IMAGE_ID = 22L;
    private static final int CHUNK_SIZE = 4;
    private static final byte[] OBJECT_BYTES = "abcdefgh".getBytes(StandardCharsets.UTF_8);

    private FileUtil fileUtil;
    private ImageUploadSessionMapper sessionMapper;
    private ImageMapper imageMapper;
    private ImageUploadChunkMapper chunkMapper;
    private OssStorageService ossStorageService;
    private ImageTransactionalService transactionalService;
    private ImageStatusService imageStatusService;
    private ImageUploadCompleteServiceImpl service;

    @BeforeEach
    void setUp() {
        fileUtil = mock(FileUtil.class);
        sessionMapper = mock(ImageUploadSessionMapper.class);
        imageMapper = mock(ImageMapper.class);
        chunkMapper = mock(ImageUploadChunkMapper.class);
        ossStorageService = mock(OssStorageService.class);
        transactionalService = mock(ImageTransactionalService.class);
        imageStatusService = mock(ImageStatusService.class);
        service = new ImageUploadCompleteServiceImpl(
                fileUtil,
                sessionMapper,
                imageMapper,
                chunkMapper,
                ossStorageService,
                transactionalService,
                imageStatusService
        );
    }

    @Test
    void uploadingSessionValidatesPartsBeforeCasThenCompletesOutsideDatabaseTransaction() {
        ImageUploadSessionPO uploading = session(ImageUploadSessionStatusEnum.UPLOADING, 7,
                LocalDateTime.now().plusHours(1), sha256(OBJECT_BYTES));
        ImageUploadSessionPO completing = session(ImageUploadSessionStatusEnum.COMPLETING, 8,
                LocalDateTime.now().plusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID))
                .thenReturn(uploading, completing);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        when(sessionMapper.updateStatusIfVersion(
                UPLOAD_ID,
                USER_ID,
                ImageUploadSessionStatusEnum.UPLOADING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                7
        )).thenReturn(1);
        stubSuccessfulObjectCompletion();

        ImageUploadCompleteVO result = service.completeUpload(UPLOAD_ID, request(), USER_ID);

        assertEquals(IMAGE_ID, result.getImageId());
        InOrder order = inOrder(chunkMapper, sessionMapper, ossStorageService, transactionalService);
        order.verify(chunkMapper).selectByUploadIdOrderByPartNumber(UPLOAD_ID);
        order.verify(sessionMapper).updateStatusIfVersion(
                UPLOAD_ID,
                USER_ID,
                ImageUploadSessionStatusEnum.UPLOADING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                7
        );
        order.verify(ossStorageService).completeMultipartUpload(
                eq(OBJECT_KEY), eq(OSS_UPLOAD_ID), any(), eq((long) OBJECT_BYTES.length));
        order.verify(ossStorageService).openObject(OBJECT_KEY);
        order.verify(transactionalService).completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);
    }

    @Test
    void casWinnerAcceptsSessionAlreadyCompletedByAnotherInstance() {
        ImageUploadSessionPO uploading = session(ImageUploadSessionStatusEnum.UPLOADING, 7,
                LocalDateTime.now().plusHours(1), sha256(OBJECT_BYTES));
        ImageUploadSessionPO completed = session(ImageUploadSessionStatusEnum.COMPLETED, 9,
                LocalDateTime.now().plusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID))
                .thenReturn(uploading, completed);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        when(sessionMapper.updateStatusIfVersion(
                UPLOAD_ID,
                USER_ID,
                ImageUploadSessionStatusEnum.UPLOADING.getCode(),
                ImageUploadSessionStatusEnum.COMPLETING.getCode(),
                7
        )).thenReturn(1);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID))
                .thenReturn(image(ImageStatusEnum.UPLOADED));

        ImageUploadCompleteVO result = service.completeUpload(UPLOAD_ID, request(), USER_ID);

        assertEquals(IMAGE_ID, result.getImageId());
        verifyNoInteractions(ossStorageService, transactionalService, imageStatusService);
    }

    @Test
    void completingSessionIgnoresOriginalExpiryAndContinuesCompletion() {
        ImageUploadSessionPO completing = session(ImageUploadSessionStatusEnum.COMPLETING, 8,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completing);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        stubSuccessfulObjectCompletion();

        ImageUploadCompleteVO result = service.completeUpload(UPLOAD_ID, request(), USER_ID);

        assertEquals(IMAGE_ID, result.getImageId());
        verify(sessionMapper, never()).updateStatusIfVersion(anyString(), anyLong(), anyInt(), anyInt(), anyInt());
        verify(ossStorageService).completeMultipartUpload(eq(OBJECT_KEY), eq(OSS_UPLOAD_ID), any(), eq(8L));
        verify(transactionalService).completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);
    }

    @Test
    void incompletePartsAreRejectedBeforeCasAndOssComplete() {
        ImageUploadSessionPO uploading = session(ImageUploadSessionStatusEnum.UPLOADING, 7,
                LocalDateTime.now().plusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(uploading);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID))
                .thenReturn(List.of(validChunks().getFirst()));

        assertThrows(ConflictException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        verify(sessionMapper, never()).updateStatusIfVersion(anyString(), anyLong(), anyInt(), anyInt(), anyInt());
        verifyNoInteractions(ossStorageService, transactionalService, imageStatusService);
    }

    @Test
    void expiredUploadingSessionIsRejectedButCompletingRuleIsNotApplied() {
        ImageUploadSessionPO uploading = session(ImageUploadSessionStatusEnum.UPLOADING, 7,
                LocalDateTime.now().minusSeconds(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(uploading);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        assertEquals("上传会话已过期", exception.getMessage());
        verifyNoInteractions(chunkMapper, ossStorageService, transactionalService, imageStatusService);
    }

    @Test
    void networkReadFailureDoesNotMarkContentInvalidOrDeleteObject() {
        ImageUploadSessionPO completing = session(ImageUploadSessionStatusEnum.COMPLETING, 8,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completing);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(ImageTransitionResultEnum.ALREADY_TARGET);
        when(ossStorageService.completeMultipartUpload(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new OssCompleteResult(OssCompleteState.COMPLETED, "etag", OBJECT_BYTES.length));
        when(ossStorageService.openObject(OBJECT_KEY))
                .thenReturn(new OssObjectResource(failingInputStream(), OBJECT_BYTES.length, "image/jpeg"));

        assertThrows(OssStorageException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        verify(transactionalService, never()).failMultipartUpload(anyString(), anyLong(), anyLong());
        verify(transactionalService, never()).completeMultipartUpload(anyString(), anyLong(), anyLong());
        verify(ossStorageService, never()).deleteObject(anyString());
        verify(ossStorageService, never()).abortMultipartUpload(anyString(), anyString());
    }

    @Test
    void hashMismatchRecordsFailureBeforeDeletingObject() {
        byte[] wrongContent = "abcdEFGH".getBytes(StandardCharsets.UTF_8);
        ImageUploadSessionPO completing = session(ImageUploadSessionStatusEnum.COMPLETING, 8,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completing);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(ImageTransitionResultEnum.ALREADY_TARGET);
        when(ossStorageService.completeMultipartUpload(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new OssCompleteResult(OssCompleteState.COMPLETED, "etag", OBJECT_BYTES.length));
        when(ossStorageService.openObject(OBJECT_KEY))
                .thenReturn(new OssObjectResource(
                        new ByteArrayInputStream(wrongContent), wrongContent.length, "image/jpeg"));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        assertEquals("完整文件SHA-256校验失败", exception.getMessage());
        InOrder order = inOrder(transactionalService, ossStorageService);
        order.verify(transactionalService).failMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);
        order.verify(ossStorageService).deleteObject(OBJECT_KEY);
        verify(transactionalService, never()).completeMultipartUpload(anyString(), anyLong(), anyLong());
        verify(ossStorageService, never()).abortMultipartUpload(anyString(), anyString());
    }

    @Test
    void completedObjectSizeMismatchRecordsFailureBeforeDeletingObject() {
        ImageUploadSessionPO completing = session(ImageUploadSessionStatusEnum.COMPLETING, 8,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completing);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(ImageTransitionResultEnum.ALREADY_TARGET);
        OssStorageException sizeMismatch = new OssStorageException(
                "COMPLETE_MULTIPART",
                OBJECT_KEY,
                new OssObjectSizeMismatchException(OBJECT_BYTES.length, OBJECT_BYTES.length - 1L)
        );
        when(ossStorageService.completeMultipartUpload(anyString(), anyString(), any(), anyLong()))
                .thenThrow(sizeMismatch);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        assertTrue(exception.getMessage().contains("对象大小不一致"));
        assertSame(sizeMismatch, exception.getSuppressed()[0]);
        InOrder order = inOrder(transactionalService, ossStorageService);
        order.verify(transactionalService).failMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);
        order.verify(ossStorageService).deleteObject(OBJECT_KEY);
        verify(ossStorageService, never()).openObject(anyString());
        verify(transactionalService, never()).completeMultipartUpload(anyString(), anyLong(), anyLong());
        verify(ossStorageService, never()).abortMultipartUpload(anyString(), anyString());
    }

    @Test
    void failureToRecordIntegrityErrorPreservesObjectAndOriginalIntegrityFailure() {
        byte[] wrongContent = "abcdEFGH".getBytes(StandardCharsets.UTF_8);
        ImageUploadSessionPO completing = session(ImageUploadSessionStatusEnum.COMPLETING, 8,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completing);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(ImageTransitionResultEnum.ALREADY_TARGET);
        when(ossStorageService.completeMultipartUpload(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new OssCompleteResult(OssCompleteState.COMPLETED, "etag", OBJECT_BYTES.length));
        when(ossStorageService.openObject(OBJECT_KEY))
                .thenReturn(new OssObjectResource(
                        new ByteArrayInputStream(wrongContent), wrongContent.length, "image/jpeg"));
        RuntimeException databaseFailure = new RuntimeException("database unavailable");
        doThrow(databaseFailure).when(transactionalService)
                .failMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        assertEquals(1, exception.getSuppressed().length);
        assertSame(databaseFailure, exception.getSuppressed()[0]);
        verify(ossStorageService, never()).deleteObject(anyString());
        verify(ossStorageService, never()).abortMultipartUpload(anyString(), anyString());
    }

    @Test
    void finalDatabaseFailureNeverDeletesOrAbortsCorrectObject() {
        ImageUploadSessionPO completing = session(ImageUploadSessionStatusEnum.COMPLETING, 8,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completing);
        when(chunkMapper.selectByUploadIdOrderByPartNumber(UPLOAD_ID)).thenReturn(validChunks());
        stubSuccessfulObjectCompletion();
        RuntimeException databaseFailure = new RuntimeException("final database update failed");
        doThrow(databaseFailure).when(transactionalService)
                .completeMultipartUpload(UPLOAD_ID, USER_ID, IMAGE_ID);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        assertSame(databaseFailure, exception);
        verify(ossStorageService, never()).deleteObject(anyString());
        verify(ossStorageService, never()).abortMultipartUpload(anyString(), anyString());
        verify(transactionalService, never()).failMultipartUpload(anyString(), anyLong(), anyLong());
    }

    @Test
    void completedSessionReturnsOnlyWhenImageIsUploadedOrReady() {
        ImageUploadSessionPO completed = session(ImageUploadSessionStatusEnum.COMPLETED, 9,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completed);
        ImagePO uploaded = image(ImageStatusEnum.UPLOADED);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(uploaded);

        ImageUploadCompleteVO result = service.completeUpload(UPLOAD_ID, request(), USER_ID);

        assertEquals(IMAGE_ID, result.getImageId());
        verifyNoInteractions(chunkMapper, ossStorageService, transactionalService, imageStatusService);
    }

    @Test
    void completedSessionWithInconsistentImageIsRejected() {
        ImageUploadSessionPO completed = session(ImageUploadSessionStatusEnum.COMPLETED, 9,
                LocalDateTime.now().minusHours(1), sha256(OBJECT_BYTES));
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(completed);
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID))
                .thenReturn(image(ImageStatusEnum.PENDING_UPLOAD));

        assertThrows(ConflictException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        verifyNoInteractions(chunkMapper, ossStorageService, transactionalService, imageStatusService);
    }

    @Test
    void missingSessionIsRejectedWithoutNullPointerException() {
        when(sessionMapper.selectByUploadIdAndUserId(UPLOAD_ID, USER_ID)).thenReturn(null);

        assertThrows(ParamIllegalException.class,
                () -> service.completeUpload(UPLOAD_ID, request(), USER_ID));

        verifyNoInteractions(chunkMapper, ossStorageService, transactionalService, imageStatusService);
    }

    private void stubSuccessfulObjectCompletion() {
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(ImageTransitionResultEnum.ALREADY_TARGET);
        when(ossStorageService.completeMultipartUpload(anyString(), anyString(), any(), anyLong()))
                .thenReturn(new OssCompleteResult(OssCompleteState.COMPLETED, "etag", OBJECT_BYTES.length));
        when(ossStorageService.openObject(OBJECT_KEY))
                .thenReturn(new OssObjectResource(
                        new ByteArrayInputStream(OBJECT_BYTES), OBJECT_BYTES.length, "image/jpeg"));
    }

    private ImageUploadCompleteReqDto request() {
        ImageUploadCompleteReqDto request = new ImageUploadCompleteReqDto();
        request.setImageId(IMAGE_ID);
        request.setSha256(sha256(OBJECT_BYTES));
        request.setTotalChunks(2);
        return request;
    }

    private ImageUploadSessionPO session(
            ImageUploadSessionStatusEnum status,
            int version,
            LocalDateTime expiresAt,
            String sha256) {
        return ImageUploadSessionPO.builder()
                .uploadId(UPLOAD_ID)
                .imageId(IMAGE_ID)
                .userId(USER_ID)
                .ossUploadId(OSS_UPLOAD_ID)
                .objectKey(OBJECT_KEY)
                .sha256(sha256)
                .fileSize((long) OBJECT_BYTES.length)
                .chunkSize(CHUNK_SIZE)
                .totalChunks(2)
                .status(status.getCode())
                .version(version)
                .expiresAt(expiresAt)
                .build();
    }

    private List<ImageUploadChunkPO> validChunks() {
        return List.of(
                ImageUploadChunkPO.builder()
                        .chunkIndex(0)
                        .partNumber(1)
                        .chunkSha256("a".repeat(64))
                        .etag("etag-1")
                        .chunkSize(CHUNK_SIZE)
                        .build(),
                ImageUploadChunkPO.builder()
                        .chunkIndex(1)
                        .partNumber(2)
                        .chunkSha256("b".repeat(64))
                        .etag("etag-2")
                        .chunkSize(CHUNK_SIZE)
                        .build()
        );
    }

    private ImagePO image(ImageStatusEnum status) {
        ImagePO image = new ImagePO();
        image.setId(IMAGE_ID);
        image.setStatus(status.getCode());
        return image;
    }

    private InputStream failingInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("network read failed");
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                throw new IOException("network read failed");
            }
        };
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
