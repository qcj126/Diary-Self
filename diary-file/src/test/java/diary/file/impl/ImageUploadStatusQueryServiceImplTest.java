package diary.file.impl;

import diary.file.entity.po.ImagePO;
import diary.file.entity.vo.ImageUploadStatusVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.service.ImageUploadStatusQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageUploadStatusQueryServiceImplTest {

    private static final Long USER_ID = 9L;
    private static final Long FIRST_IMAGE_ID = 10001L;
    private static final Long SECOND_IMAGE_ID = 10002L;

    private ImageMapper imageMapper;
    private ImageUploadStatusQueryService service;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        service = new ImageUploadStatusQueryServiceImpl(imageMapper);
    }

    @Test
    void queryStatusesPreservesRequestOrderAndMapsStatusSemantics() {
        ImagePO uploading = image(FIRST_IMAGE_ID, ImageStatusEnum.UPLOADING);
        ImagePO ready = image(SECOND_IMAGE_ID, ImageStatusEnum.READY);
        List<Long> requestedIds = List.of(FIRST_IMAGE_ID, SECOND_IMAGE_ID);

        when(imageMapper.selectImageStatusesByIdsAndUserId(requestedIds, USER_ID))
                .thenReturn(List.of(ready, uploading));

        List<ImageUploadStatusVO> result = service.queryStatuses(requestedIds, USER_ID);

        assertEquals(List.of(FIRST_IMAGE_ID, SECOND_IMAGE_ID),
                result.stream().map(ImageUploadStatusVO::getImageId).toList());

        ImageUploadStatusVO uploadingStatus = result.get(0);
        assertEquals(ImageStatusEnum.UPLOADING.getCode(), uploadingStatus.getStatusCode());
        assertEquals(ImageStatusEnum.UPLOADING.name(), uploadingStatus.getStatus());
        assertEquals(ImageStatusEnum.UPLOADING.getDescription(), uploadingStatus.getDescription());
        assertFalse(uploadingStatus.getTerminal());
        assertFalse(uploadingStatus.getSuccess());

        ImageUploadStatusVO readyStatus = result.get(1);
        assertEquals(ImageStatusEnum.READY.getCode(), readyStatus.getStatusCode());
        assertEquals(ImageStatusEnum.READY.name(), readyStatus.getStatus());
        assertEquals(ImageStatusEnum.READY.getDescription(), readyStatus.getDescription());
        assertTrue(readyStatus.getTerminal());
        assertTrue(readyStatus.getSuccess());
    }

    @Test
    void queryStatusesDeduplicatesIdsBeforeQueryAndKeepsFirstSeenOrder() {
        List<Long> distinctIds = List.of(FIRST_IMAGE_ID, SECOND_IMAGE_ID);
        when(imageMapper.selectImageStatusesByIdsAndUserId(distinctIds, USER_ID))
                .thenReturn(List.of(
                        image(FIRST_IMAGE_ID, ImageStatusEnum.PENDING_UPLOAD),
                        image(SECOND_IMAGE_ID, ImageStatusEnum.UPLOADED)
                ));

        List<ImageUploadStatusVO> result = service.queryStatuses(
                List.of(FIRST_IMAGE_ID, FIRST_IMAGE_ID, SECOND_IMAGE_ID),
                USER_ID
        );

        assertEquals(distinctIds,
                result.stream().map(ImageUploadStatusVO::getImageId).toList());
        verify(imageMapper).selectImageStatusesByIdsAndUserId(distinctIds, USER_ID);
    }

    @Test
    void queryStatusesOmitsMissingOrUnauthorizedImages() {
        List<Long> requestedIds = List.of(FIRST_IMAGE_ID, SECOND_IMAGE_ID);
        when(imageMapper.selectImageStatusesByIdsAndUserId(requestedIds, USER_ID))
                .thenReturn(List.of(image(FIRST_IMAGE_ID, ImageStatusEnum.READY)));

        List<ImageUploadStatusVO> result = service.queryStatuses(requestedIds, USER_ID);

        assertEquals(1, result.size());
        assertEquals(FIRST_IMAGE_ID, result.get(0).getImageId());
    }

    @Test
    void queryStatusesRejectsEmptyIdsBeforeMapperCall() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.queryStatuses(List.of(), USER_ID)
        );

        verifyNoInteractions(imageMapper);
    }

    @Test
    void queryStatusesRejectsNullUserBeforeMapperCall() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.queryStatuses(List.of(FIRST_IMAGE_ID), null)
        );

        verifyNoInteractions(imageMapper);
    }

    private ImagePO image(Long imageId, ImageStatusEnum status) {
        ImagePO image = new ImagePO();
        image.setId(imageId);
        image.setStatus(status.getCode());
        return image;
    }
}
