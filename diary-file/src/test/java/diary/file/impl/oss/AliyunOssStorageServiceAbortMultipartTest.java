package diary.file.impl.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.VoidResult;
import diary.file.exception.OssStorageException;
import diary.file.property.ImageUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AliyunOssStorageServiceAbortMultipartTest {

    private static final String BUCKET_NAME = "diary-image-bucket";
    private static final String OBJECT_KEY = "images/2026/11/abort.jpg";
    private static final String OSS_UPLOAD_ID = "upload-id-abort-001";

    private OSS ossClient;
    private AliyunOssStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        ossClient = mock(OSS.class);
        ImageUploadProperties properties = new ImageUploadProperties();
        properties.setChunkSize(262_144);
        properties.setMaxImageSize(100 * 1024 * 1024);
        storageService = new AliyunOssStorageServiceImpl(
                ossClient,
                BUCKET_NAME,
                properties
        );
    }

    @Test
    void abortPassesBucketKeyAndUploadIdAndEndsNormally() {
        assertDoesNotThrow(
                () -> storageService.abortMultipartUpload(OBJECT_KEY, OSS_UPLOAD_ID)
        );

        ArgumentCaptor<AbortMultipartUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(ossClient).abortMultipartUpload(requestCaptor.capture());
        AbortMultipartUploadRequest request = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, request.getBucketName());
        assertEquals(OBJECT_KEY, request.getKey());
        assertEquals(OSS_UPLOAD_ID, request.getUploadId());
        verifyNoObjectOperations();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void abortRejectsBlankObjectKeyWithoutCallingOss(String objectKey) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.abortMultipartUpload(objectKey, OSS_UPLOAD_ID)
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void abortRejectsBlankUploadIdWithoutCallingOss(String uploadId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.abortMultipartUpload(OBJECT_KEY, uploadId)
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void abortIgnoresNoSuchUploadByErrorCode() {
        OSSException noSuchUpload = ossException("another message", "NoSuchUpload");
        doThrow(noSuchUpload)
                .when(ossClient)
                .abortMultipartUpload(any(AbortMultipartUploadRequest.class));

        assertDoesNotThrow(
                () -> storageService.abortMultipartUpload(OBJECT_KEY, OSS_UPLOAD_ID)
        );

        verifyNoObjectOperations();
    }

    @Test
    void abortDoesNotUseExceptionMessageToDetectNoSuchUpload() {
        OSSException sameMessageButDifferentCode =
                ossException("NoSuchUpload", "AccessDenied");
        doThrow(sameMessageButDifferentCode)
                .when(ossClient)
                .abortMultipartUpload(any(AbortMultipartUploadRequest.class));

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.abortMultipartUpload(OBJECT_KEY, OSS_UPLOAD_ID)
        );

        assertAbortException(exception, sameMessageButDifferentCode);
    }

    @Test
    void abortWrapsOssExceptionWithoutErrorCode() {
        OSSException sdkException = new OSSException("ordinary OSS error");
        doThrow(sdkException)
                .when(ossClient)
                .abortMultipartUpload(any(AbortMultipartUploadRequest.class));

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.abortMultipartUpload(OBJECT_KEY, OSS_UPLOAD_ID)
        );

        assertAbortException(exception, sdkException);
    }

    @Test
    void abortWrapsClientException() {
        ClientException sdkException = new ClientException("client error");
        doThrow(sdkException)
                .when(ossClient)
                .abortMultipartUpload(any(AbortMultipartUploadRequest.class));

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.abortMultipartUpload(OBJECT_KEY, OSS_UPLOAD_ID)
        );

        assertAbortException(exception, sdkException);
    }

    @Test
    void secondAbortAlsoIgnoresNoSuchUpload() {
        when(ossClient.abortMultipartUpload(any(AbortMultipartUploadRequest.class)))
                .thenReturn(new VoidResult())
                .thenThrow(ossException("already aborted", "NoSuchUpload"));

        assertDoesNotThrow(
                () -> {
                    storageService.abortMultipartUpload(OBJECT_KEY, OSS_UPLOAD_ID);
                    storageService.abortMultipartUpload(OBJECT_KEY, OSS_UPLOAD_ID);
                }
        );

        verify(ossClient, never()).deleteObject(BUCKET_NAME, OBJECT_KEY);
        verify(ossClient, never()).doesObjectExist(BUCKET_NAME, OBJECT_KEY);
    }

    private OSSException ossException(String message, String errorCode) {
        return new OSSException(
                message,
                errorCode,
                "request-id",
                "host-id",
                "multipart-upload",
                null,
                "DELETE"
        );
    }

    private void assertAbortException(
            OssStorageException exception,
            Throwable expectedCause
    ) {
        assertSame(expectedCause, exception.getCause());
        assertEquals(
                "OSS操作失败，operation=ABORT_MULTIPART，objectKey=" + OBJECT_KEY,
                exception.getMessage()
        );
        verifyNoObjectOperations();
    }

    private void verifyNoObjectOperations() {
        verify(ossClient, never()).deleteObject(BUCKET_NAME, OBJECT_KEY);
        verify(ossClient, never()).doesObjectExist(BUCKET_NAME, OBJECT_KEY);
    }
}
