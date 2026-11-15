package diary.file.impl.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import diary.file.entity.model.OssObjectResource;
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
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AliyunOssStorageServiceObjectOperationsTest {

    private static final String BUCKET_NAME = "diary-image-bucket";
    private static final String OBJECT_KEY = "images/2026/11/object.jpg";
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final int MAX_IMAGE_SIZE = 1_048_576;

    private OSS ossClient;
    private AliyunOssStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        ossClient = mock(OSS.class);
        ImageUploadProperties properties = new ImageUploadProperties();
        properties.setChunkSize(262_144);
        properties.setMaxImageSize(MAX_IMAGE_SIZE);
        storageService = new AliyunOssStorageServiceImpl(
                ossClient,
                BUCKET_NAME,
                properties
        );
    }

    @Test
    void openObjectPassesBucketAndKeyAndWrapsSdkObject() throws IOException {
        InputStream content = mock(InputStream.class);
        ObjectMetadata metadata = metadata(3L, CONTENT_TYPE);
        OSSObject ossObject = ossObject(content, metadata);
        when(ossClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(ossObject);
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenReturn(metadata);

        OssObjectResource resource = storageService.openObject(OBJECT_KEY);

        assertSame(content, resource.getInputStream());
        assertEquals(3L, resource.getContentLength());
        assertEquals(CONTENT_TYPE, resource.getContentType());
        verify(ossClient).getObject(BUCKET_NAME, OBJECT_KEY);

        resource.close();
        verify(content).close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void openObjectRejectsBlankKeyWithoutCallingOss(String objectKey) {
        assertThrows(IllegalArgumentException.class, () -> storageService.openObject(objectKey));

        verifyNoInteractions(ossClient);
    }

    @Test
    void openObjectWrapsOssException() {
        OSSException sdkException = new OSSException("GET failed");
        when(ossClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.openObject(OBJECT_KEY)
        );

        assertStorageException(exception, "GET_OBJECT", sdkException);
    }

    @Test
    void openObjectWrapsClientException() {
        ClientException sdkException = new ClientException("GET client failed");
        when(ossClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.openObject(OBJECT_KEY)
        );

        assertStorageException(exception, "GET_OBJECT", sdkException);
    }

    @Test
    void openObjectRejectsNullSdkResult() {
        when(ossClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(null);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.openObject(OBJECT_KEY)
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(storageErrorMessage("GET_OBJECT"), exception.getMessage());
    }

    @Test
    void openObjectRejectsNullContentAndClosesSdkObject() throws IOException {
        OSSObject ossObject = ossObject(null, metadata(1L, CONTENT_TYPE));
        when(ossClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(ossObject);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.openObject(OBJECT_KEY)
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        verify(ossObject).close();
    }

    @Test
    void openObjectRejectsNullMetadataAndClosesSdkObject() throws IOException {
        OSSObject ossObject = ossObject(new ByteArrayInputStream(new byte[]{1}), null);
        when(ossClient.getObject(BUCKET_NAME, OBJECT_KEY)).thenReturn(ossObject);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.openObject(OBJECT_KEY)
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        verify(ossObject).close();
    }

    @Test
    void objectExistsReturnsTrue() {
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenReturn(true);

        assertTrue(storageService.objectExists(OBJECT_KEY));
    }

    @Test
    void objectExistsReturnsFalse() {
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenReturn(false);

        assertFalse(storageService.objectExists(OBJECT_KEY));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void objectExistsRejectsBlankKeyWithoutCallingOss(String objectKey) {
        assertThrows(IllegalArgumentException.class, () -> storageService.objectExists(objectKey));

        verifyNoInteractions(ossClient);
    }

    @Test
    void objectExistsWrapsOssExceptionInsteadOfReturningFalse() {
        OSSException sdkException = new OSSException("exists failed");
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.objectExists(OBJECT_KEY)
        );

        assertStorageException(exception, "OBJECT_EXISTS", sdkException);
    }

    @Test
    void objectExistsWrapsClientExceptionInsteadOfReturningFalse() {
        ClientException sdkException = new ClientException("exists client failed");
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.objectExists(OBJECT_KEY)
        );

        assertStorageException(exception, "OBJECT_EXISTS", sdkException);
    }

    @Test
    void putObjectPassesRequestDataAndReturnsEtag() {
        InputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(ossClient.putObject(
                eq(BUCKET_NAME),
                eq(OBJECT_KEY),
                same(inputStream),
                any(ObjectMetadata.class)
        )).thenReturn(putResult("etag-put-001"));

        String etag = storageService.putObject(
                OBJECT_KEY, CONTENT_TYPE, inputStream, 3L
        );

        assertEquals("etag-put-001", etag);
        ArgumentCaptor<ObjectMetadata> metadataCaptor =
                ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(ossClient).putObject(
                eq(BUCKET_NAME),
                eq(OBJECT_KEY),
                same(inputStream),
                metadataCaptor.capture()
        );
        assertEquals(CONTENT_TYPE, metadataCaptor.getValue().getContentType());
        assertEquals(3L, metadataCaptor.getValue().getContentLength());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void putObjectRejectsBlankKeyWithoutCallingOss(String objectKey) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.putObject(
                        objectKey, CONTENT_TYPE, validInputStream(), 1L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void putObjectRejectsBlankContentTypeWithoutCallingOss(String contentType) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.putObject(
                        OBJECT_KEY, contentType, validInputStream(), 1L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void putObjectRejectsNullInputStreamWithoutCallingOss() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.putObject(OBJECT_KEY, CONTENT_TYPE, null, 1L)
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, 1_048_577L})
    void putObjectRejectsInvalidLengthWithoutCallingOss(long contentLength) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.putObject(
                        OBJECT_KEY, CONTENT_TYPE, validInputStream(), contentLength
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void putObjectWrapsOssException() {
        OSSException sdkException = new OSSException("PUT failed");
        when(ossClient.putObject(
                eq(BUCKET_NAME),
                eq(OBJECT_KEY),
                any(InputStream.class),
                any(ObjectMetadata.class)
        )).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.putObject(
                        OBJECT_KEY, CONTENT_TYPE, validInputStream(), 1L
                )
        );

        assertStorageException(exception, "PUT_OBJECT", sdkException);
    }

    @Test
    void putObjectWrapsClientException() {
        ClientException sdkException = new ClientException("PUT client failed");
        when(ossClient.putObject(
                eq(BUCKET_NAME),
                eq(OBJECT_KEY),
                any(InputStream.class),
                any(ObjectMetadata.class)
        )).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.putObject(
                        OBJECT_KEY, CONTENT_TYPE, validInputStream(), 1L
                )
        );

        assertStorageException(exception, "PUT_OBJECT", sdkException);
    }

    @Test
    void putObjectRejectsNullSdkResult() {
        when(ossClient.putObject(
                eq(BUCKET_NAME),
                eq(OBJECT_KEY),
                any(InputStream.class),
                any(ObjectMetadata.class)
        )).thenReturn(null);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.putObject(
                        OBJECT_KEY, CONTENT_TYPE, validInputStream(), 1L
                )
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void putObjectRejectsBlankEtag(String etag) {
        when(ossClient.putObject(
                eq(BUCKET_NAME),
                eq(OBJECT_KEY),
                any(InputStream.class),
                any(ObjectMetadata.class)
        )).thenReturn(putResult(etag));

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.putObject(
                        OBJECT_KEY, CONTENT_TYPE, validInputStream(), 1L
                )
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    void putObjectDoesNotCloseCallerInputStream() {
        CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[]{1});
        when(ossClient.putObject(
                eq(BUCKET_NAME),
                eq(OBJECT_KEY),
                same(inputStream),
                any(ObjectMetadata.class)
        )).thenReturn(putResult("etag-open-stream"));

        storageService.putObject(OBJECT_KEY, CONTENT_TYPE, inputStream, 1L);

        assertFalse(inputStream.closed);
    }

    @Test
    void deleteObjectPassesBucketAndKeyAndEndsNormally() {
        assertDoesNotThrow(() -> storageService.deleteObject(OBJECT_KEY));

        verify(ossClient).deleteObject(BUCKET_NAME, OBJECT_KEY);
        verifyNoDeleteSideOperations();
    }

    @Test
    void deleteObjectIgnoresNoSuchKey() {
        doThrow(ossException("missing", "NoSuchKey"))
                .when(ossClient)
                .deleteObject(BUCKET_NAME, OBJECT_KEY);

        assertDoesNotThrow(() -> storageService.deleteObject(OBJECT_KEY));

        verifyNoDeleteSideOperations();
    }

    @Test
    void deleteObjectWrapsOtherOssException() {
        OSSException sdkException = ossException("denied", "AccessDenied");
        doThrow(sdkException).when(ossClient).deleteObject(BUCKET_NAME, OBJECT_KEY);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.deleteObject(OBJECT_KEY)
        );

        assertStorageException(exception, "DELETE_OBJECT", sdkException);
        verifyNoDeleteSideOperations();
    }

    @Test
    void deleteObjectWrapsClientException() {
        ClientException sdkException = new ClientException("delete client failed");
        doThrow(sdkException).when(ossClient).deleteObject(BUCKET_NAME, OBJECT_KEY);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.deleteObject(OBJECT_KEY)
        );

        assertStorageException(exception, "DELETE_OBJECT", sdkException);
        verifyNoDeleteSideOperations();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void deleteObjectRejectsBlankKeyWithoutCallingOss(String objectKey) {
        assertThrows(IllegalArgumentException.class, () -> storageService.deleteObject(objectKey));

        verifyNoInteractions(ossClient);
    }

    private OSSObject ossObject(InputStream content, ObjectMetadata metadata) {
        OSSObject ossObject = mock(OSSObject.class);
        when(ossObject.getObjectContent()).thenReturn(content);
        when(ossObject.getObjectMetadata()).thenReturn(metadata);
        return ossObject;
    }

    private ObjectMetadata metadata(long contentLength, String contentType) {
        ObjectMetadata metadata = mock(ObjectMetadata.class);
        when(metadata.getContentLength()).thenReturn(contentLength);
        when(metadata.getContentType()).thenReturn(contentType);
        return metadata;
    }

    private PutObjectResult putResult(String etag) {
        PutObjectResult result = new PutObjectResult();
        result.setETag(etag);
        return result;
    }

    private OSSException ossException(String message, String errorCode) {
        return new OSSException(
                message,
                errorCode,
                "request-id",
                "host-id",
                "object",
                null,
                "DELETE"
        );
    }

    private ByteArrayInputStream validInputStream() {
        return new ByteArrayInputStream(new byte[]{1});
    }

    private void assertStorageException(
            OssStorageException exception,
            String operation,
            Throwable expectedCause
    ) {
        assertSame(expectedCause, exception.getCause());
        assertEquals(storageErrorMessage(operation), exception.getMessage());
    }

    private String storageErrorMessage(String operation) {
        return "OSS操作失败，operation=" + operation + "，objectKey=" + OBJECT_KEY;
    }

    private void verifyNoDeleteSideOperations() {
        verify(ossClient, never()).doesObjectExist(BUCKET_NAME, OBJECT_KEY);
        verify(ossClient, never())
                .abortMultipartUpload(any(AbortMultipartUploadRequest.class));
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
