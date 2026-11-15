package diary.file.impl.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import diary.file.entity.model.OssObjectMetadata;
import diary.file.exception.OssStorageException;
import diary.file.property.ImageUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AliyunOssStorageServiceObjectMetadataTest {

    private static final String BUCKET_NAME = "diary-image-bucket";
    private static final String OBJECT_KEY = "images/2026/11/metadata.jpg";

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
    void getObjectMetadataPassesKeyAndMapsAllFields() {
        ObjectMetadata sdkMetadata = metadata(12_345L, "image/jpeg", "etag-001");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenReturn(sdkMetadata);

        OssObjectMetadata result = storageService.getObjectMetadata(OBJECT_KEY);

        assertEquals(new OssObjectMetadata(12_345L, "image/jpeg", "etag-001"), result);
        verify(ossClient).getObjectMetadata(BUCKET_NAME, OBJECT_KEY);
        verifyNoObjectLookupOrDownload();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void getObjectMetadataRejectsBlankObjectKeyWithoutCallingOss(String objectKey) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.getObjectMetadata(objectKey)
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void getObjectMetadataWrapsOssExceptionAsHeadObject() {
        OSSException sdkException = new OSSException("HEAD failed");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.getObjectMetadata(OBJECT_KEY)
        );

        assertHeadException(exception, sdkException);
    }

    @Test
    void getObjectMetadataWrapsClientExceptionAsHeadObject() {
        ClientException sdkException = new ClientException("HEAD client failed");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.getObjectMetadata(OBJECT_KEY)
        );

        assertHeadException(exception, sdkException);
    }

    @Test
    void getObjectMetadataRejectsNullSdkResultWithIllegalStateCause() {
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenReturn(null);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.getObjectMetadata(OBJECT_KEY)
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(headErrorMessage(), exception.getMessage());
        verifyNoObjectLookupOrDownload();
    }

    @Test
    void getObjectMetadataAllowsNullContentType() {
        ObjectMetadata sdkMetadata = metadata(123L, null, "etag-001");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenReturn(sdkMetadata);

        OssObjectMetadata result = storageService.getObjectMetadata(OBJECT_KEY);

        assertEquals(123L, result.contentLength());
        assertNull(result.contentType());
        assertEquals("etag-001", result.etag());
        verifyNoObjectLookupOrDownload();
    }

    @Test
    void getObjectMetadataAllowsNullEtag() {
        ObjectMetadata sdkMetadata = metadata(123L, "image/png", null);
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenReturn(sdkMetadata);

        OssObjectMetadata result = storageService.getObjectMetadata(OBJECT_KEY);

        assertEquals(123L, result.contentLength());
        assertEquals("image/png", result.contentType());
        assertNull(result.etag());
        verifyNoObjectLookupOrDownload();
    }

    private ObjectMetadata metadata(long contentLength, String contentType, String etag) {
        ObjectMetadata metadata = mock(ObjectMetadata.class);
        when(metadata.getContentLength()).thenReturn(contentLength);
        when(metadata.getContentType()).thenReturn(contentType);
        when(metadata.getETag()).thenReturn(etag);
        return metadata;
    }

    private void assertHeadException(
            OssStorageException exception,
            Throwable expectedCause
    ) {
        assertSame(expectedCause, exception.getCause());
        assertEquals(headErrorMessage(), exception.getMessage());
        verifyNoObjectLookupOrDownload();
    }

    private String headErrorMessage() {
        return "OSS操作失败，operation=HEAD_OBJECT，objectKey=" + OBJECT_KEY;
    }

    private void verifyNoObjectLookupOrDownload() {
        verify(ossClient, never()).doesObjectExist(BUCKET_NAME, OBJECT_KEY);
        verify(ossClient, never()).getObject(BUCKET_NAME, OBJECT_KEY);
    }
}
