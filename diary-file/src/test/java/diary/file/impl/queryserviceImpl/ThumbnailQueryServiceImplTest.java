package diary.file.impl.queryserviceImpl;

import diary.file.entity.model.OssObjectResource;
import diary.file.entity.po.ImagePO;
import diary.file.entity.po.ThumbnailPO;
import diary.file.entity.vo.ImageVO;
import diary.file.enums.ImageStatusEnum;
import diary.file.enums.ThumbnailStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mapper.ThumbnailMapper;
import diary.file.service.oss.OssStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThumbnailQueryServiceImplTest {
    private static final long IMAGE_ID = 22L;
    private static final long USER_ID = 11L;

    private ImageMapper imageMapper;
    private ThumbnailMapper thumbnailMapper;
    private OssStorageService ossStorageService;
    private ThumbnailQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        thumbnailMapper = mock(ThumbnailMapper.class);
        ossStorageService = mock(OssStorageService.class);
        service = new ThumbnailQueryServiceImpl(imageMapper, thumbnailMapper, ossStorageService);
    }

    @Test
    void returnsStableProtectedThumbnailUrlForReadyOwnedImage() {
        when(thumbnailMapper.selectReadyByImageIdsAndUserId(
                List.of(IMAGE_ID), USER_ID, List.of(1000, 2000, 9000),
                ImageStatusEnum.READY.getCode(), ThumbnailStatusEnum.READY.getCode()
        )).thenReturn(List.of(thumbnail()));

        List<ImageVO> result = service.queryThumbnailUrls(List.of(IMAGE_ID), USER_ID);

        assertEquals(1, result.size());
        assertEquals(IMAGE_ID, result.getFirst().getId());
        assertEquals("/file/thumbnails/22", result.getFirst().getUrl());
        verify(imageMapper, never()).selectForUploadByIdAndUserId(IMAGE_ID, USER_ID);
    }

    @Test
    void nonThumbnailTypeIsNotExposed() {
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(image(3000));

        assertNull(service.openThumbnail(IMAGE_ID, USER_ID));

        verify(thumbnailMapper, never()).selectByImageIdAndUserId(IMAGE_ID, USER_ID);
        verify(ossStorageService, never()).openObject(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void opensReadyThumbnailObjectAfterAuthorityChecks() {
        ThumbnailPO thumbnail = thumbnail();
        OssObjectResource resource = new OssObjectResource(
                new ByteArrayInputStream(new byte[]{1}), 1, "image/jpeg"
        );
        when(imageMapper.selectForUploadByIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(image(1000));
        when(thumbnailMapper.selectByImageIdAndUserId(IMAGE_ID, USER_ID)).thenReturn(thumbnail);
        when(ossStorageService.openObject(thumbnail.getObjectKey())).thenReturn(resource);

        assertEquals(resource, service.openThumbnail(IMAGE_ID, USER_ID));
    }

    private ImagePO image(int type) {
        ImagePO image = new ImagePO();
        image.setId(IMAGE_ID);
        image.setUserId(USER_ID);
        image.setType(type);
        image.setStatus(ImageStatusEnum.READY.getCode());
        return image;
    }

    private ThumbnailPO thumbnail() {
        return ThumbnailPO.builder()
                .imageId(IMAGE_ID)
                .userId(USER_ID)
                .status(ThumbnailStatusEnum.READY.getCode())
                .deleted(0)
                .objectKey("images/thumbnail/11/22/v1.jpg")
                .build();
    }
}
