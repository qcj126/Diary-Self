package diary.file.impl.oss;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import diary.file.entity.model.OssCompleteResult;
import diary.file.entity.model.OssUploadedPart;
import diary.file.enums.OssCompleteState;
import diary.file.exception.OssStorageException;
import diary.file.property.ImageUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AliyunOssStorageServiceCompleteMultipartTest {

    private static final String BUCKET_NAME = "diary-image-bucket";
    private static final String OBJECT_KEY = "images/2026/11/complete.jpg";
    private static final String OSS_UPLOAD_ID = "upload-id-complete-001";
    private static final int CHUNK_SIZE = 262_144;
    private static final int MAX_IMAGE_SIZE = 1_048_576;

    private OSS ossClient;
    private AliyunOssStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        ossClient = mock(OSS.class);
        ImageUploadProperties properties = new ImageUploadProperties();
        properties.setChunkSize(CHUNK_SIZE);
        properties.setMaxImageSize(MAX_IMAGE_SIZE);
        storageService = new AliyunOssStorageServiceImpl(
                ossClient,
                BUCKET_NAME,
                properties
        );
    }

    @Test
    void completeSortsPartsPreservesEtagsAndInputAndReturnsCompleted() {
        List<OssUploadedPart> parts = new ArrayList<>(List.of(
                part(3, "etag-3-original", 300L),
                part(1, "etag-1-original", 100L),
                part(2, "etag-2-original", 200L)
        ));
        List<OssUploadedPart> originalOrder = List.copyOf(parts);
        ObjectMetadata completedMetadata = metadata(600L, "completed-etag");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY))
                .thenReturn(completedMetadata);

        OssCompleteResult result = storageService.completeMultipartUpload(
                OBJECT_KEY,
                OSS_UPLOAD_ID,
                parts,
                600L
        );

        assertEquals(
                new OssCompleteResult(OssCompleteState.COMPLETED, "completed-etag", 600L),
                result
        );
        assertEquals(originalOrder, parts);

        ArgumentCaptor<CompleteMultipartUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(ossClient).completeMultipartUpload(requestCaptor.capture());
        CompleteMultipartUploadRequest request = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, request.getBucketName());
        assertEquals(OBJECT_KEY, request.getKey());
        assertEquals(OSS_UPLOAD_ID, request.getUploadId());

        List<PartETag> partETags = request.getPartETags();
        assertEquals(List.of(1, 2, 3), partETags.stream().map(PartETag::getPartNumber).toList());
        assertEquals(
                List.of("etag-1-original", "etag-2-original", "etag-3-original"),
                partETags.stream().map(PartETag::getETag).toList()
        );
    }

    @Test
    void completeRejectsNullPartsWithoutCallingOss() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, null, 100L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeRejectsEmptyPartsWithoutCallingOss() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, List.of(), 100L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeRejectsNullPartWithoutCallingOss() {
        List<OssUploadedPart> parts = Arrays.asList(part(1, "etag-1", 100L), null);

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, 100L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10_001})
    void completeRejectsOutOfRangePartNumberWithoutCallingOss(int partNumber) {
        List<OssUploadedPart> parts = List.of(part(partNumber, "etag", 100L));

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, 100L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeRejectsDuplicatePartNumberWithoutCallingOss() {
        List<OssUploadedPart> parts = List.of(
                part(1, "etag-1", 100L),
                part(1, "etag-duplicate", 200L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, 300L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeRejectsNonContinuousPartNumbersWithoutCallingOss() {
        List<OssUploadedPart> parts = List.of(
                part(1, "etag-1", 100L),
                part(3, "etag-3", 200L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, 300L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void completeRejectsBlankEtagWithoutCallingOss(String etag) {
        List<OssUploadedPart> parts = List.of(part(1, etag, 100L));

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, 100L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void completeRejectsNonPositivePartSizeWithoutCallingOss(long partSize) {
        List<OssUploadedPart> parts = List.of(part(1, "etag", partSize));

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, 100L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeRejectsPartSizeAboveChunkSizeWithoutCallingOss() {
        long oversizedPart = CHUNK_SIZE + 1L;
        List<OssUploadedPart> parts = List.of(part(1, "etag", oversizedPart));

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, oversizedPart
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeRejectsPartSizeSumMismatchWithoutCallingOss() {
        List<OssUploadedPart> parts = List.of(
                part(1, "etag-1", 100L),
                part(2, "etag-2", 200L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, parts, 301L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void completeRejectsNonPositiveExpectedFileSizeWithoutCallingOss(long expectedFileSize) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), expectedFileSize
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeRejectsExpectedFileSizeAboveMaximumWithoutCallingOss() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), MAX_IMAGE_SIZE + 1L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void completeRejectsBlankObjectKeyWithoutCallingOss(String objectKey) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        objectKey, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void completeRejectsBlankUploadIdWithoutCallingOss(String uploadId) {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, uploadId, validParts(), 300L
                )
        );

        verifyNoInteractions(ossClient);
    }

    @Test
    void completeWrapsOrdinaryOssException() {
        OSSException sdkException = new OSSException("complete failed");
        when(ossClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertCompleteException(exception, sdkException);
    }

    @Test
    void completeWrapsClientException() {
        ClientException sdkException = new ClientException("client failed");
        when(ossClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(sdkException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertCompleteException(exception, sdkException);
    }

    @Test
    void completeRejectsCompletedObjectWithUnexpectedSize() {
        ObjectMetadata wrongSizeMetadata = metadata(299L, "wrong-size-etag");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY))
                .thenReturn(wrongSizeMetadata);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(completeErrorMessage(), exception.getMessage());
    }

    @Test
    void completeKeepsCompleteOperationWhenMetadataLoaderReturnsNull() {
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenReturn(null);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(completeErrorMessage(), exception.getMessage());
    }

    @Test
    void noSuchUploadWithExistingCorrectObjectReturnsAlreadyCompleted() {
        OSSException noSuchUpload = noSuchUploadException();
        when(ossClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(noSuchUpload);
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenReturn(true);
        ObjectMetadata existingMetadata = metadata(300L, "existing-etag");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY))
                .thenReturn(existingMetadata);

        OssCompleteResult result = storageService.completeMultipartUpload(
                OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
        );

        assertEquals(
                new OssCompleteResult(
                        OssCompleteState.ALREADY_COMPLETED,
                        "existing-etag",
                        300L
                ),
                result
        );
    }

    @Test
    void noSuchUploadWithoutExistingObjectFailsWithOriginalException() {
        OSSException noSuchUpload = noSuchUploadException();
        when(ossClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(noSuchUpload);
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenReturn(false);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertCompleteException(exception, noSuchUpload);
        verify(ossClient, never()).getObjectMetadata(BUCKET_NAME, OBJECT_KEY);
    }

    @Test
    void noSuchUploadWithExistingObjectOfWrongSizeFails() {
        when(ossClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(noSuchUploadException());
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenReturn(true);
        ObjectMetadata wrongSizeMetadata = metadata(299L, "wrong-size-etag");
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY))
                .thenReturn(wrongSizeMetadata);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals(completeErrorMessage(), exception.getMessage());
    }

    @Test
    void noSuchUploadWrapsObjectExistenceCheckFailure() {
        ClientException existenceException = new ClientException("exists failed");
        when(ossClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(noSuchUploadException());
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY))
                .thenThrow(existenceException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertCompleteException(exception, existenceException);
    }

    @Test
    void noSuchUploadWrapsHeadObjectFailure() {
        OSSException headException = new OSSException("HEAD failed");
        when(ossClient.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(noSuchUploadException());
        when(ossClient.doesObjectExist(BUCKET_NAME, OBJECT_KEY)).thenReturn(true);
        when(ossClient.getObjectMetadata(BUCKET_NAME, OBJECT_KEY)).thenThrow(headException);

        OssStorageException exception = assertThrows(
                OssStorageException.class,
                () -> storageService.completeMultipartUpload(
                        OBJECT_KEY, OSS_UPLOAD_ID, validParts(), 300L
                )
        );

        assertCompleteException(exception, headException);
    }

    private List<OssUploadedPart> validParts() {
        return List.of(
                part(1, "etag-1", 100L),
                part(2, "etag-2", 200L)
        );
    }

    private OssUploadedPart part(int partNumber, String etag, long partSize) {
        return new OssUploadedPart(partNumber, etag, partSize);
    }

    private ObjectMetadata metadata(long contentLength, String etag) {
        ObjectMetadata metadata = mock(ObjectMetadata.class);
        when(metadata.getContentLength()).thenReturn(contentLength);
        when(metadata.getETag()).thenReturn(etag);
        return metadata;
    }

    private OSSException noSuchUploadException() {
        return new OSSException(
                "No such upload",
                "NoSuchUpload",
                "request-id",
                "host-id",
                "multipart-upload",
                null,
                "POST"
        );
    }

    private void assertCompleteException(
            OssStorageException exception,
            Throwable expectedCause
    ) {
        assertSame(expectedCause, exception.getCause());
        assertEquals(completeErrorMessage(), exception.getMessage());
    }

    private String completeErrorMessage() {
        return "OSS操作失败，operation=COMPLETE_MULTIPART，objectKey=" + OBJECT_KEY;
    }
}
