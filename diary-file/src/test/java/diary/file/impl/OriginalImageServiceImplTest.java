package diary.file.impl;

import diary.file.entity.model.OssObjectResource;
import diary.file.entity.po.ImagePO;
import diary.file.enums.ImageStatusEnum;
import diary.file.mapper.ImageMapper;
import diary.file.service.oss.OssStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OriginalImageServiceImplTest {
    private ImageMapper imageMapper;
    private OssStorageService ossStorageService;
    private OriginalImageServiceImpl service;

    @BeforeEach
    void setUp() {
        imageMapper = mock(ImageMapper.class);
        ossStorageService = mock(OssStorageService.class);
        service = new OriginalImageServiceImpl(imageMapper, ossStorageService);
    }

    @Test
    void opensReadyOwnedImageDirectlyFromOss() {
        ImagePO image = image(ImageStatusEnum.READY.getCode());
        OssObjectResource resource = new OssObjectResource(
                new ByteArrayInputStream(new byte[]{1}), 1, "image/jpeg"
        );
        when(imageMapper.selectForUploadByIdAndUserId(22L, 11L)).thenReturn(image);
        when(ossStorageService.openObject(image.getObjectKey())).thenReturn(resource);

        assertSame(resource, service.openByImageId(22L, 11L));
    }

    @Test
    void rejectsImageThatIsNotReady() {
        when(imageMapper.selectForUploadByIdAndUserId(22L, 11L))
                .thenReturn(image(ImageStatusEnum.UPLOADED.getCode()));

        assertNull(service.openByImageId(22L, 11L));
        verify(ossStorageService, never()).openObject("images/original/11/22.jpg");
    }

    @Test
    void legacyFileNameLookupStillUsesCurrentUser() {
        ImagePO image = image(ImageStatusEnum.READY.getCode());
        when(imageMapper.selectReadyByOriginalNameAndUserId("photo.jpg", 11L)).thenReturn(image);

        service.openByOriginalName("photo.jpg", 11L);

        verify(imageMapper).selectReadyByOriginalNameAndUserId("photo.jpg", 11L);
    }

    private ImagePO image(int status) {
        ImagePO image = new ImagePO();
        image.setId(22L);
        image.setUserId(11L);
        image.setStatus(status);
        image.setObjectKey("images/original/11/22.jpg");
        return image;
    }
}
