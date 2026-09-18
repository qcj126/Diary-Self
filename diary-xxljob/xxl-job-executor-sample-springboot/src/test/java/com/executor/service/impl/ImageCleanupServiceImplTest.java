package com.executor.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.executor.mapper.ImageCleanupMapper;
import diary.common.entity.xxlJob.ImageCleanUpResultDTO;
import diary.common.entity.xxlJob.ImageCleanupRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageCleanupServiceImplTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String OBJECT_KEY = "daily_path_test.jpg";

    @Mock
    private ImageCleanupMapper imageCleanupMapper;

    @Mock
    private OSS ossClient;

    private ImageCleanupServiceImpl imageCleanupService;

    @BeforeEach
    void setUp() {
        imageCleanupService = new ImageCleanupServiceImpl(
                imageCleanupMapper,
                ossClient,
                BUCKET_NAME,
                24,
                100,
                List.of()
        );
    }

    @Test
    void shouldNotRetryOutsideOssSdkWhenDeleteFails() {
        ImageCleanupRecord image = new ImageCleanupRecord();
        image.setId(1L);
        image.setObjectKey(OBJECT_KEY);

        when(imageCleanupMapper.selectUnreferencedImages(24, 100)).thenReturn(List.of(image));
        when(imageCleanupMapper.deleteImageByIdIfUnreferenced(1L, 24)).thenReturn(1);
        doThrow(new ClientException("network error"))
                .when(ossClient).deleteObject(BUCKET_NAME, OBJECT_KEY);

        ImageCleanUpResultDTO result = imageCleanupService.cleanupUnreferencedImages();

        assertEquals(1, result.getScannedCount());
        assertEquals(0, result.getDeletedCount());
        assertEquals(1, result.getFailedCount());
        verify(ossClient, times(1)).deleteObject(BUCKET_NAME, OBJECT_KEY);
    }
}
