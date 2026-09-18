package com.executor.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObjectSummary;
import com.executor.mapper.ImageCleanupMapper;
import com.executor.service.ImageCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import diary.common.entity.xxlJob.ImageCleanUpResultDTO;
import diary.common.entity.xxlJob.ImageCleanupRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ImageCleanupServiceImpl implements ImageCleanupService {

    private static final int OSS_LIST_PAGE_SIZE = 1_000;

    private final ImageCleanupMapper imageCleanupMapper;
    private final OSS ossClient;
    private final String bucketName;
    private final int retentionHours;
    private final int batchSize;
    private final List<String> ossPrefixes;

    public ImageCleanupServiceImpl(ImageCleanupMapper imageCleanupMapper,
                                   OSS ossClient,
                                   @Value("${aliyun.oss.bucket-name}") String bucketName,
                                   @Value("${image.cleanup.retention-hours:24}") int retentionHours,
                                   @Value("${image.cleanup.batch-size:200}") int batchSize,
                                   @Value("${image.cleanup.oss-prefixes:food_path_,cook_path_,ingredient_path_,bean_matching_path_,gift_path_,snack_path_,tea_path_,travel_path_,daily_path_,exercise_path_,walk_path}")
                                   List<String> ossPrefixes) {
        this.imageCleanupMapper = imageCleanupMapper;
        this.ossClient = ossClient;
        this.bucketName = bucketName;
        this.retentionHours = Math.max(retentionHours, 1);
        this.batchSize = Math.max(batchSize, 1);
        this.ossPrefixes = ossPrefixes == null ? List.of() : ossPrefixes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    @Override
    public ImageCleanUpResultDTO cleanupUnreferencedImages() {
        CleanupCounter counter = new CleanupCounter();
        List<ImageCleanupRecord> images = imageCleanupMapper.selectUnreferencedImages(retentionHours, batchSize);
        counter.scannedCount += images.size();

        for (ImageCleanupRecord image : images) {
            try {
                if (deleteDatabaseOrphan(image)) {
                    counter.deletedCount++;
                }
            } catch (Exception e) {
                counter.failedCount++;
                Long imageId = image == null ? null : image.getId();
                String objectKey = image == null ? null : image.getObjectKey();
                log.error("清理数据库未关联图片失败，id: {}, objectKey: {}", imageId, objectKey, e);
                XxlJobHelper.log("图片清理失败，id: " + imageId + "，原因: " + e.getMessage());
            }
        }
        int remainingDeletes = batchSize;
        Instant expireBefore = Instant.now().minus(retentionHours, ChronoUnit.HOURS);

        for (String prefix : ossPrefixes) {
            if (remainingDeletes <= 0) {
                return new ImageCleanUpResultDTO(counter.scannedCount, counter.deletedCount, counter.failedCount);
            }
            try {
                remainingDeletes = cleanupOssPrefix(prefix, expireBefore, remainingDeletes, counter);
            } catch (Exception e) {
                counter.failedCount++;
                log.error("扫描 OSS 未登记图片失败，prefix: {}", prefix, e);
                XxlJobHelper.log("扫描 OSS 前缀失败，prefix: " + prefix + "，原因: " + e.getMessage());
            }
        }
        return new ImageCleanUpResultDTO(counter.scannedCount, counter.deletedCount, counter.failedCount);
    }

    private boolean deleteDatabaseOrphan(ImageCleanupRecord image) {
        int deleted = imageCleanupMapper.deleteImageByIdIfUnreferenced(image.getId(), retentionHours);
         if (deleted <= 0) {
            XxlJobHelper.log("图片在清理期间已被业务关联，跳过清理，id: " + image.getId());
            return false;
        }

        String objectKey = image.getObjectKey();
        if (StringUtils.hasText(objectKey)) {
            deleteOssObject(objectKey);
        } else {
            XxlJobHelper.log("图片 objectKey 为空，仅删除数据库记录，id: " + image.getId());
        }

        XxlJobHelper.log("图片清理成功，id: " + image.getId() + "，objectKey: " + objectKey);
        return true;
    }

    private int cleanupOssPrefix(String prefix, Instant expireBefore, int remainingDeletes,
                                 CleanupCounter counter) {
        String continuationToken = null;
        do {
            ListObjectsV2Request request = new ListObjectsV2Request(bucketName);
            request.setPrefix(prefix);
            request.setMaxKeys(OSS_LIST_PAGE_SIZE);
            request.setContinuationToken(continuationToken);

            ListObjectsV2Result result = ossClient.listObjectsV2(request);
            List<String> staleObjectKeys = result.getObjectSummaries().stream()
                    .filter(summary -> summary.getLastModified() != null && summary.getLastModified().toInstant().isBefore(expireBefore))
                    .map(OSSObjectSummary::getKey)
                    .filter(StringUtils::hasText)
                    .toList();
            counter.scannedCount += staleObjectKeys.size();

            Set<String> registeredObjectKeys = imageCleanupMapper.selectImagesByObjectKeys(List.copyOf(staleObjectKeys)).stream()
                    .map(ImageCleanupRecord::getObjectKey)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toCollection(HashSet::new));;

            for (String objectKey : staleObjectKeys) {
                if (remainingDeletes <= 0) {
                    return 0;
                }
                if (!registeredObjectKeys.contains(objectKey)) {
                    try {
                        deleteOssObject(objectKey);
                        counter.deletedCount++;
                        remainingDeletes--;
                        XxlJobHelper.log("已删除 OSS 未登记图片，objectKey: " + objectKey);
                    } catch (Exception e) {
                        counter.failedCount++;
                        log.error("删除 OSS 未登记图片失败，objectKey: {}", objectKey, e);
                        XxlJobHelper.log("OSS 未登记图片删除失败，objectKey: " + objectKey
                                + "，原因: " + e.getMessage());
                    }
                }
            }

            continuationToken = result.isTruncated() ? result.getNextContinuationToken() : null;
        } while (continuationToken != null && remainingDeletes > 0);
        return remainingDeletes;
    }

    private void deleteOssObject(String objectKey) {
        try {
            ossClient.deleteObject(bucketName, objectKey);
        } catch (OSSException | ClientException e) {
            throw new IllegalStateException(
                    "删除 OSS 文件失败，bucketName: " + bucketName + "，objectKey: " + objectKey,
                    e
            );
        }
    }

    private static final class CleanupCounter {
        private int scannedCount;
        private int deletedCount;
        private int failedCount;
    }
}
