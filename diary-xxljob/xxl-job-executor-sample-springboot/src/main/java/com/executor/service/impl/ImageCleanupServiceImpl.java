package com.executor.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.executor.mapper.ImageCleanupMapper;
import com.executor.service.ImageCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import diary.common.entity.xxlJob.ImageCleanUpResultDTO;
import diary.common.entity.xxlJob.ImageCleanupRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageCleanupServiceImpl implements ImageCleanupService {

    private static final int MAX_DELETE_ATTEMPTS = 3;

    @Resource
    private ImageCleanupMapper imageCleanupMapper;
    @Resource
    private OSS ossClient;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Override
    public ImageCleanUpResultDTO cleanupUnreferencedImages() {
        List<ImageCleanupRecord> images = imageCleanupMapper.selectUnreferencedImages();
        int deletedCount = 0;
        int failedCount = 0;

        for (ImageCleanupRecord image : images) {
            try {
                deleteImage(image);
                deletedCount++;
            } catch (Exception e) {
                failedCount++;
                Long imageId = image == null ? null : image.getId();
                String objectKey = image == null ? null : image.getObjectKey();
                log.error("Failed to cleanup image, id: {}, objectKey: {}", imageId, objectKey, e);
                XxlJobHelper.log("图片清理失败，id: " + imageId + "，原因: " + e.getMessage());
            }
        }

        return new ImageCleanUpResultDTO(images.size(), deletedCount, failedCount);
    }

    private void deleteImage(ImageCleanupRecord image) {
        if (image == null || image.getId() == null) {
            throw new IllegalArgumentException("image id must not be null");
        }

        String objectKey = image.getObjectKey();
        if (StringUtils.hasText(objectKey)) {
            deleteOssObjectWithRetry(objectKey);
        } else {
            XxlJobHelper.log("图片 objectKey 为空，仅删除数据库记录，id: " + image.getId());
        }

        int deleted = imageCleanupMapper.deleteImageById(image.getId());
        if (deleted <= 0) {
            throw new IllegalStateException("image record delete failed, id: " + image.getId());
        }

        XxlJobHelper.log("图片清理成功，id: " + image.getId() + "，objectKey: " + objectKey);
    }

    private void deleteOssObjectWithRetry(String objectKey) {
        for (int attempt = 1; attempt <= MAX_DELETE_ATTEMPTS; attempt++) {
            try {
                ossClient.deleteObject(bucketName, objectKey);
                return;
            } catch (OSSException e) {
                if ("NoSuchKey".equals(e.getErrorCode())) {
                    XxlJobHelper.log("OSS 文件不存在，继续删除数据库记录，objectKey: " + objectKey);
                    return;
                }
                if ("AccessDenied".equals(e.getErrorCode()) || "NoSuchBucket".equals(e.getErrorCode())) {
                    throw new IllegalStateException("OSS delete failed and cannot retry, objectKey: " + objectKey, e);
                }
                if (attempt == MAX_DELETE_ATTEMPTS) {
                    throw new IllegalStateException("OSS delete failed after retries, objectKey: " + objectKey, e);
                }
            } catch (ClientException e) {
                if (attempt == MAX_DELETE_ATTEMPTS) {
                    throw new IllegalStateException("OSS delete failed after retries, objectKey: " + objectKey, e);
                }
            }

            waitBeforeRetry(attempt);
        }
    }

    private void waitBeforeRetry(int attempt) {
        try {
            TimeUnit.SECONDS.sleep(1L << (attempt - 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OSS delete retry interrupted", e);
        }
    }
}
