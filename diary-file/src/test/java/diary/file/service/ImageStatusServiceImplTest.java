package diary.file.service;

import diary.common.exception.NullResultException;
import diary.file.entity.po.ImagePO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.exception.ImageStatusConflictException;
import diary.file.impl.ImageStatusServiceImpl;
import diary.file.mapper.ImageMapper;
import diary.file.state.ImageStatusTransitionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static diary.file.enums.ImageStatusEnum.PENDING_UPLOAD;
import static diary.file.enums.ImageStatusEnum.READY;
import static diary.file.enums.ImageStatusEnum.RETRY_WAIT;
import static diary.file.enums.ImageStatusEnum.UPLOADED;
import static diary.file.enums.ImageStatusEnum.UPLOADING;
import static diary.file.enums.ImageStatusEnum.UPLOAD_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageStatusServiceImplTest {

    private static final Long IMAGE_ID = 10001L;

    private ImageMapper imageMapper;
    private ImageStatusService imageStatusService;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        imageStatusService = new ImageStatusServiceImpl(
                new ImageStatusTransitionPolicy(),
                imageMapper
        );
    }

    @Test
    void markUploadingUsesPendingAsExpectedStatus() {
        assertSuccessfulTransition(PENDING_UPLOAD, UPLOADING, () -> imageStatusService.markUploading(IMAGE_ID));
    }

    @Test
    void markUploadedUsesUploadingAsExpectedStatus() {
        assertSuccessfulTransition(UPLOADING, UPLOADED, () -> imageStatusService.markUploaded(IMAGE_ID));
    }

    @Test
    void markReadyUsesUploadedAsExpectedStatus() {
        assertSuccessfulTransition(UPLOADED, READY, () -> imageStatusService.markReady(IMAGE_ID));
    }

    @Test
    void markRetryWaitUsesUploadingAsExpectedStatus() {
        assertSuccessfulTransition(UPLOADING, RETRY_WAIT, () -> imageStatusService.markRetryWait(IMAGE_ID));
    }

    @Test
    void markUploadFailedUsesPendingAsExpectedStatus() {
        assertSuccessfulTransition(PENDING_UPLOAD, UPLOAD_FAILED,
                () -> imageStatusService.markUploadFailed(IMAGE_ID));
    }

    @Test
    void sameTargetStatusIsIdempotent() {
        when(imageMapper.selectImageStatusById(IMAGE_ID)).thenReturn(imageIn(UPLOADING));

        assertEquals(
                ImageTransitionResultEnum.ALREADY_TARGET,
                imageStatusService.markUploading(IMAGE_ID)
        );

        verify(imageMapper, never()).updateStatusIfCurrent(anyLong(), anyInt(), anyInt());
    }

    @Test
    void illegalTransitionIsRejectedWithoutDatabaseUpdate() {
        when(imageMapper.selectImageStatusById(IMAGE_ID)).thenReturn(imageIn(READY));

        assertEquals(
                ImageTransitionResultEnum.REJECTED,
                imageStatusService.markUploading(IMAGE_ID)
        );

        verify(imageMapper, never()).updateStatusIfCurrent(anyLong(), anyInt(), anyInt());
    }

    @Test
    void nullImageIdIsRejectedBeforeQuery() {
        assertThrows(IllegalArgumentException.class, () -> imageStatusService.markUploading(null));

        verifyNoInteractions(imageMapper);
    }

    @Test
    void missingImageIsRejected() {
        when(imageMapper.selectImageStatusById(IMAGE_ID)).thenReturn(null);

        assertThrows(NullResultException.class, () -> imageStatusService.markUploading(IMAGE_ID));

        verify(imageMapper, never()).updateStatusIfCurrent(anyLong(), anyInt(), anyInt());
    }

    @Test
    void casMissIsIdempotentWhenAnotherInstanceReachedTarget() {
        when(imageMapper.selectImageStatusById(IMAGE_ID))
                .thenReturn(imageIn(PENDING_UPLOAD), imageIn(UPLOADING));
        when(imageMapper.updateStatusIfCurrent(
                IMAGE_ID, PENDING_UPLOAD.getCode(), UPLOADING.getCode()))
                .thenReturn(0);

        assertEquals(
                ImageTransitionResultEnum.ALREADY_TARGET,
                imageStatusService.markUploading(IMAGE_ID)
        );

        verify(imageMapper, times(2)).selectImageStatusById(IMAGE_ID);
    }

    @Test
    void casMissThrowsWhenAnotherInstanceReachedDifferentTarget() {
        when(imageMapper.selectImageStatusById(IMAGE_ID))
                .thenReturn(imageIn(PENDING_UPLOAD), imageIn(UPLOAD_FAILED));
        when(imageMapper.updateStatusIfCurrent(
                IMAGE_ID, PENDING_UPLOAD.getCode(), UPLOADING.getCode()))
                .thenReturn(0);

        ImageStatusConflictException exception = assertThrows(
                ImageStatusConflictException.class,
                () -> imageStatusService.markUploading(IMAGE_ID)
        );

        assertTrue(exception.getMessage().contains(String.valueOf(IMAGE_ID)));
        assertTrue(exception.getMessage().contains(PENDING_UPLOAD.name()));
        assertTrue(exception.getMessage().contains(UPLOADING.name()));
        assertTrue(exception.getMessage().contains(UPLOAD_FAILED.name()));
    }

    @Test
    void casMissThrowsWhenImageWasDeleted() {
        when(imageMapper.selectImageStatusById(IMAGE_ID))
                .thenReturn(imageIn(PENDING_UPLOAD), null);
        when(imageMapper.updateStatusIfCurrent(
                IMAGE_ID, PENDING_UPLOAD.getCode(), UPLOADING.getCode()))
                .thenReturn(0);

        assertThrows(NullResultException.class, () -> imageStatusService.markUploading(IMAGE_ID));

        verify(imageMapper, times(2)).selectImageStatusById(IMAGE_ID);
    }

    private void assertSuccessfulTransition(
            ImageStatusEnum currentStatus,
            ImageStatusEnum targetStatus,
            Supplier<ImageTransitionResultEnum> command) {
        when(imageMapper.selectImageStatusById(IMAGE_ID)).thenReturn(imageIn(currentStatus));
        when(imageMapper.updateStatusIfCurrent(
                IMAGE_ID, currentStatus.getCode(), targetStatus.getCode()))
                .thenReturn(1);

        assertEquals(ImageTransitionResultEnum.APPLIED, command.get());

        verify(imageMapper).updateStatusIfCurrent(
                IMAGE_ID, currentStatus.getCode(), targetStatus.getCode());
    }

    private ImagePO imageIn(ImageStatusEnum status) {
        ImagePO image = new ImagePO();
        image.setId(IMAGE_ID);
        image.setStatus(status.getCode());
        return image;
    }
}
