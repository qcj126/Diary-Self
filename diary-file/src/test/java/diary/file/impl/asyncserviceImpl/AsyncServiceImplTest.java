package diary.file.impl.asyncserviceImpl;

import com.aliyun.oss.OSS;
import diary.file.entity.dto.ImageDTO;
import diary.file.entity.po.ImagePO;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageStatusService;
import diary.file.service.filetransactionalservice.FileTransactionalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.List;

import static diary.file.enums.ImageTransitionResultEnum.APPLIED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AsyncServiceImplTest {
    private static final Long IMAGE_ID = 10001L;
    private static final String BUCKET_NAME = "test-bucket";
    private static final String OBJECT_KEY = "images/test.jpg";
    private static final String ORIGINAL_NAME = "test.jpg";

    private ImageMapper imageMapper;
    private ImageStatusService imageStatusService;
    private FileTransactionalService fileTransactionalService;
    private OSS ossClient;
    private AsyncServiceImpl asyncService;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        imageStatusService = mock(ImageStatusService.class);
        fileTransactionalService = mock(FileTransactionalService.class);
        ossClient = mock(OSS.class);

        asyncService = new AsyncServiceImpl();
        ReflectionTestUtils.setField(asyncService, "imageMapper", imageMapper);
        ReflectionTestUtils.setField(asyncService, "imageStatusService", imageStatusService);
        ReflectionTestUtils.setField(asyncService, "fileTransactionService", fileTransactionalService);
        ReflectionTestUtils.setField(asyncService, "diaryOssClient", ossClient);
        ReflectionTestUtils.setField(asyncService, "bucketName", BUCKET_NAME);

        ImagePO image = new ImagePO();
        image.setId(IMAGE_ID);
        image.setObjectKey(OBJECT_KEY);
        when(imageMapper.selectImagesForUploadByIds(List.of(IMAGE_ID))).thenReturn(List.of(image));
    }

    @Test
    void recordsUploadedImageOnceAfterOssSucceeds() {
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(APPLIED);

        invokeUpload();

        var ordered = inOrder(imageStatusService, ossClient, fileTransactionalService);
        ordered.verify(imageStatusService).markUploading(IMAGE_ID);
        ordered.verify(ossClient).putObject(eq(BUCKET_NAME), eq(OBJECT_KEY), any(File.class));
        ordered.verify(fileTransactionalService).recordImageUploaded(
                eq(IMAGE_ID), eq(OBJECT_KEY), eq(ORIGINAL_NAME), anyList());
        ordered.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @EnumSource(value = ImageTransitionResultEnum.class, names = {"ALREADY_TARGET", "REJECTED"})
    void doesNotUploadOrRecordOutboxWithoutUploadClaim(ImageTransitionResultEnum claimResult) {
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(claimResult);

        invokeUpload();

        verifyNoInteractions(ossClient, fileTransactionalService);
    }

    @Test
    void doesNotRecordOutboxWhenOssUploadFails() {
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(APPLIED);
        when(ossClient.putObject(eq(BUCKET_NAME), eq(OBJECT_KEY), any(File.class)))
                .thenThrow(new RuntimeException("OSS unavailable"));

        invokeUpload();

        verify(imageStatusService).markRetryWait(IMAGE_ID);
        verifyNoInteractions(fileTransactionalService);
    }

    @Test
    void doesNotMarkUploadFailedWhenOutboxRecordingFailsAfterOssSuccess() {
        when(imageStatusService.markUploading(IMAGE_ID)).thenReturn(APPLIED);
        doThrow(new RuntimeException("database unavailable"))
                .when(fileTransactionalService)
                .recordImageUploaded(eq(IMAGE_ID), eq(OBJECT_KEY), eq(ORIGINAL_NAME), anyList());

        assertDoesNotThrow(this::invokeUpload);

        verify(ossClient).putObject(eq(BUCKET_NAME), eq(OBJECT_KEY), any(File.class));
        verify(fileTransactionalService).recordImageUploaded(
                eq(IMAGE_ID), eq(OBJECT_KEY), eq(ORIGINAL_NAME), anyList());
        verify(imageStatusService, never()).markUploadFailed(IMAGE_ID);
        verify(imageStatusService, never()).markRetryWait(IMAGE_ID);
    }

    private void invokeUpload() {
        ImageDTO imageDTO = new ImageDTO();
        imageDTO.setCode(1000);
        asyncService.uploadAndSendMsgAsync(
                List.of(IMAGE_ID), List.of(new File(ORIGINAL_NAME)), List.of(ORIGINAL_NAME), imageDTO);
    }
}
