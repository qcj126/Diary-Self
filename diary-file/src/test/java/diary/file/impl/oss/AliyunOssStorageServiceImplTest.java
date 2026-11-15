package diary.file.impl.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import diary.file.entity.model.OssUploadedPart;
import diary.file.exception.OssStorageException;
import diary.file.property.ImageUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AliyunOssStorageServiceImplTest {

    private static final String BUCKET_NAME = "diary-image-bucket";
    private static final String OBJECT_KEY = "images/2026/11/test.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final String OSS_UPLOAD_ID = "upload-id-001";
    private static final int DEFAULT_CHUNK_SIZE = 256 * 1024;
    private static final int MAX_CHUNK_SIZE = 1024 * 1024;
    private static final int MAX_IMAGE_SIZE = 100 * 1024 * 1024;

    private OSS ossClient;
    private ImageUploadProperties imageUploadProperties;
    private AliyunOssStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        ossClient = mock(OSS.class);
        imageUploadProperties = imageUploadProperties(DEFAULT_CHUNK_SIZE);
        storageService = new AliyunOssStorageServiceImpl(
                ossClient,
                BUCKET_NAME,
                imageUploadProperties
        );
    }

    @Test
    void initiateMultipartUploadPassesBucketKeyAndContentTypeAndReturnsUploadId() {
        InitiateMultipartUploadResult result = resultWithUploadId("upload-id-001");
        when(ossClient.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenReturn(result);

        String uploadId = storageService.initiateMultipartUpload(OBJECT_KEY, CONTENT_TYPE);

        assertEquals("upload-id-001", uploadId);
        ArgumentCaptor<InitiateMultipartUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
        verify(ossClient).initiateMultipartUpload(requestCaptor.capture());

        InitiateMultipartUploadRequest request = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, request.getBucketName());
        assertEquals(OBJECT_KEY, request.getKey());
        assertEquals(CONTENT_TYPE, request.getObjectMetadata().getContentType());
    }

    @Test
    void initiateMultipartUploadWrapsOssException() {
        OSSException sdkException = new OSSException("OSS service error");
        when(ossClient.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.initiateMultipartUpload(OBJECT_KEY, CONTENT_TYPE)
        );

        assertSame(sdkException, exception.getCause());
        assertEquals(
                "OSS操作失败，operation=INITIATE_MULTIPART，objectKey=" + OBJECT_KEY,
                exception.getMessage()
        );
    }

    @Test
    void initiateMultipartUploadWrapsClientException() {
        ClientException sdkException = new ClientException("OSS client error");
        when(ossClient.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.initiateMultipartUpload(OBJECT_KEY, CONTENT_TYPE)
        );

        assertSame(sdkException, exception.getCause());
        assertEquals(
                "OSS操作失败，operation=INITIATE_MULTIPART，objectKey=" + OBJECT_KEY,
                exception.getMessage()
        );
    }

    @Test
    void initiateMultipartUploadRejectsNullSdkResult() {
        when(ossClient.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenReturn(null);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.initiateMultipartUpload(OBJECT_KEY, CONTENT_TYPE)
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void initiateMultipartUploadRejectsMissingUploadId(String uploadId) {
        when(ossClient.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenReturn(resultWithUploadId(uploadId));

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.initiateMultipartUpload(OBJECT_KEY, CONTENT_TYPE)
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void initiateMultipartUploadRejectsBlankObjectKey(String objectKey) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.initiateMultipartUpload(objectKey, CONTENT_TYPE)
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void initiateMultipartUploadRejectsBlankContentType(String contentType) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.initiateMultipartUpload(OBJECT_KEY, contentType)
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void constructorRejectsNullOssClient() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AliyunOssStorageServiceImpl(null, BUCKET_NAME, imageUploadProperties)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void constructorRejectsBlankBucketName(String bucketName) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AliyunOssStorageServiceImpl(ossClient, bucketName, imageUploadProperties)
        );

        verify(ossClient, never()).initiateMultipartUpload(any(InitiateMultipartUploadRequest.class));
    }

    @Test
    void constructorRejectsNullImageUploadProperties() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AliyunOssStorageServiceImpl(ossClient, BUCKET_NAME, null)
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 1_048_577})
    void constructorRejectsChunkSizeOutsideAllowedRange(int chunkSize) {
        ImageUploadProperties invalidProperties = imageUploadProperties(chunkSize);

        assertThrows(
                IllegalArgumentException.class,
                () -> new AliyunOssStorageServiceImpl(ossClient, BUCKET_NAME, invalidProperties)
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 262_143, 262_144, 524_288, 1_048_576})
    void constructorAcceptsChunkSizeUpTo1Mb(int chunkSize) {
        ImageUploadProperties properties = imageUploadProperties(chunkSize);

        new AliyunOssStorageServiceImpl(ossClient, BUCKET_NAME, properties);

        verifyNoInteractions(ossClient);
    }

    @Test
    void constructorRejectsNullMaxImageSize() {
        ImageUploadProperties invalidProperties = new ImageUploadProperties();
        invalidProperties.setChunkSize(DEFAULT_CHUNK_SIZE);

        assertThrows(
                IllegalArgumentException.class,
                () -> new AliyunOssStorageServiceImpl(ossClient, BUCKET_NAME, invalidProperties)
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void constructorRejectsNonPositiveMaxImageSize(int maxImageSize) {
        ImageUploadProperties invalidProperties = imageUploadProperties(DEFAULT_CHUNK_SIZE);
        invalidProperties.setMaxImageSize(maxImageSize);

        assertThrows(
                IllegalArgumentException.class,
                () -> new AliyunOssStorageServiceImpl(ossClient, BUCKET_NAME, invalidProperties)
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void uploadPartPassesAllRequestFieldsAndMapsResult() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(ossClient.uploadPart(any(UploadPartRequest.class)))
                .thenReturn(uploadPartResult("etag-001"));

        OssUploadedPart uploadedPart = storageService.uploadPart(
                OBJECT_KEY,
                OSS_UPLOAD_ID,
                7,
                inputStream,
                3L
        );

        assertEquals(new OssUploadedPart(7, "etag-001", 3L), uploadedPart);
        ArgumentCaptor<UploadPartRequest> requestCaptor =
                ArgumentCaptor.forClass(UploadPartRequest.class);
        verify(ossClient).uploadPart(requestCaptor.capture());

        UploadPartRequest request = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, request.getBucketName());
        assertEquals(OBJECT_KEY, request.getKey());
        assertEquals(OSS_UPLOAD_ID, request.getUploadId());
        assertEquals(7, request.getPartNumber());
        assertSame(inputStream, request.getInputStream());
        assertEquals(3L, request.getPartSize());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void uploadPartRejectsBlankObjectKeyWithoutCallingOss(String objectKey) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.uploadPart(
                        objectKey, OSS_UPLOAD_ID, 1, validInputStream(), 1L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void uploadPartRejectsBlankUploadIdWithoutCallingOss(String uploadId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY, uploadId, 1, validInputStream(), 1L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void uploadPartRejectsNullInputStreamWithoutCallingOss() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.uploadPart(OBJECT_KEY, OSS_UPLOAD_ID, 1, null, 1L)
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10_001})
    void uploadPartRejectsOutOfRangePartNumberWithoutCallingOss(int partNumber) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY, OSS_UPLOAD_ID, partNumber, validInputStream(), 1L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10_000})
    void uploadPartAcceptsBoundaryPartNumbers(int partNumber) {
        when(ossClient.uploadPart(any(UploadPartRequest.class)))
                .thenReturn(uploadPartResult("etag-boundary"));

        OssUploadedPart uploadedPart = storageService.uploadPart(
                OBJECT_KEY, OSS_UPLOAD_ID, partNumber, validInputStream(), 1L
        );

        assertEquals(partNumber, uploadedPart.partNumber());
        ArgumentCaptor<UploadPartRequest> requestCaptor =
                ArgumentCaptor.forClass(UploadPartRequest.class);
        verify(ossClient).uploadPart(requestCaptor.capture());
        assertEquals(partNumber, requestCaptor.getValue().getPartNumber());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void uploadPartRejectsNonPositivePartSizeWithoutCallingOss(long partSize) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY, OSS_UPLOAD_ID, 1, validInputStream(), partSize
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void uploadPartRejectsPartSizeAboveConfiguredChunkSizeWithoutCallingOss() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY,
                        OSS_UPLOAD_ID,
                        1,
                        validInputStream(),
                        DEFAULT_CHUNK_SIZE + 1L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void uploadPartAcceptsConfiguredChunkSizeBoundary() {
        when(ossClient.uploadPart(any(UploadPartRequest.class)))
                .thenReturn(uploadPartResult("etag-max-size"));

        OssUploadedPart uploadedPart = storageService.uploadPart(
                OBJECT_KEY,
                OSS_UPLOAD_ID,
                1,
                validInputStream(),
                DEFAULT_CHUNK_SIZE
        );

        assertEquals(DEFAULT_CHUNK_SIZE, uploadedPart.partSize());
    }

    @Test
    void uploadPartUsesUpdatedChunkSizeUpTo1Mb() {
        ImageUploadProperties properties = imageUploadProperties(MAX_CHUNK_SIZE);
        AliyunOssStorageServiceImpl oneMegabyteChunkService =
                new AliyunOssStorageServiceImpl(ossClient, BUCKET_NAME, properties);
        when(ossClient.uploadPart(any(UploadPartRequest.class)))
                .thenReturn(uploadPartResult("etag-one-megabyte"));

        OssUploadedPart uploadedPart = oneMegabyteChunkService.uploadPart(
                OBJECT_KEY,
                OSS_UPLOAD_ID,
                1,
                validInputStream(),
                MAX_CHUNK_SIZE
        );

        assertEquals(MAX_CHUNK_SIZE, uploadedPart.partSize());
    }

    @Test
    void uploadPartWrapsOssExceptionWithUploadPartOperation() {
        OSSException sdkException = new OSSException("OSS service error");
        when(ossClient.uploadPart(any(UploadPartRequest.class))).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY, OSS_UPLOAD_ID, 1, validInputStream(), 1L
                )
        );

        assertSame(sdkException, exception.getCause());
        assertEquals(
                "OSS操作失败，operation=UPLOAD_PART，objectKey=" + OBJECT_KEY,
                exception.getMessage()
        );
    }

    @Test
    void uploadPartWrapsClientExceptionWithUploadPartOperation() {
        ClientException sdkException = new ClientException("OSS client error");
        when(ossClient.uploadPart(any(UploadPartRequest.class))).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY, OSS_UPLOAD_ID, 1, validInputStream(), 1L
                )
        );

        assertSame(sdkException, exception.getCause());
        assertEquals(
                "OSS操作失败，operation=UPLOAD_PART，objectKey=" + OBJECT_KEY,
                exception.getMessage()
        );
    }

    @Test
    void uploadPartRejectsNullSdkResult() {
        when(ossClient.uploadPart(any(UploadPartRequest.class))).thenReturn(null);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY, OSS_UPLOAD_ID, 1, validInputStream(), 1L
                )
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(
                "OSS操作失败，operation=UPLOAD_PART，objectKey=" + OBJECT_KEY,
                exception.getMessage()
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void uploadPartRejectsMissingEtag(String etag) {
        when(ossClient.uploadPart(any(UploadPartRequest.class)))
                .thenReturn(uploadPartResult(etag));

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.uploadPart(
                        OBJECT_KEY, OSS_UPLOAD_ID, 1, validInputStream(), 1L
                )
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void uploadPartDoesNotCloseCallerInputStream() {
        CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[]{1});
        when(ossClient.uploadPart(any(UploadPartRequest.class)))
                .thenReturn(uploadPartResult("etag-open-stream"));

        storageService.uploadPart(OBJECT_KEY, OSS_UPLOAD_ID, 1, inputStream, 1L);

        assertFalse(inputStream.closed);
    }

    private InitiateMultipartUploadResult resultWithUploadId(String uploadId) {
        InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
        result.setUploadId(uploadId);
        return result;
    }

    private UploadPartResult uploadPartResult(String etag) {
        UploadPartResult result = new UploadPartResult();
        result.setETag(etag);
        return result;
    }

    private ByteArrayInputStream validInputStream() {
        return new ByteArrayInputStream(new byte[]{1});
    }

    private ImageUploadProperties imageUploadProperties(int chunkSize) {
        ImageUploadProperties properties = new ImageUploadProperties();
        properties.setChunkSize(chunkSize);
        properties.setMaxImageSize(MAX_IMAGE_SIZE);
        return properties;
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private CloseTrackingInputStream(byte[] buffer) {
            super(buffer);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
