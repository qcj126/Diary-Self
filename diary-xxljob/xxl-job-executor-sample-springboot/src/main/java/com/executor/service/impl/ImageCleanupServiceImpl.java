package com.executor.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObjectSummary;
import com.executor.mapper.ImageCleanupMapper;
import com.executor.service.ImageCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import com.executor.entity.ImageCleanupRecord;
import diary.utils.OSS.OssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCleanupServiceImpl implements ImageCleanupService {

    private static final int OSS_LIST_PAGE_SIZE = 1_000;

    private final ImageCleanupMapper imageCleanupMapper;
    private final OSS ossClient;
    @Value("${aliyun.oss.bucket-name}")
    private final String bucketName;
    @Value("${image.cleanup.retention-hours:24}")
    private final int retentionHours;
    @Value("${image.cleanup.batch-size:200}")
    private final int batchSize;
    @Value("${image.cleanup.oss-prefixes:food_path_,cook_path_,ingredient_path_,bean_matching_path_,gift_path_,snack_path_,tea_path_,travel_path_,daily_path_,exercise_path_,walk_path}")
    private final List<String> ossPrefixes;
    private final OssUtil ossUtil;

    @Override
    public Integer cleanupUnreferencedImages() {
        int deletedCount = 0;
        List<ImageCleanupRecord> images = imageCleanupMapper.selectUnreferencedImages(retentionHours, batchSize);

        for (ImageCleanupRecord image : images) {
            try {
                if (deleteDatabaseOrphan(image)) {
                    deletedCount++;
                }
            } catch (Exception e) {
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
                return deletedCount;
            }
            try {
                remainingDeletes = cleanupOssPrefix(prefix, expireBefore, remainingDeletes, deletedCount);
            } catch (Exception e) {
                log.error("扫描 OSS 未登记图片失败，prefix: {}", prefix, e);
                XxlJobHelper.log("扫描 OSS 前缀失败，prefix: " + prefix + "，原因: " + e.getMessage());
            }
        }
        return deletedCount;
    }

    private boolean deleteDatabaseOrphan(ImageCleanupRecord image) {
        int deleted = imageCleanupMapper.deleteImageByIdIfUnreferenced(image.getId(), retentionHours);
         if (deleted == 0) {
            XxlJobHelper.log("图片在清理期间已被业务关联，跳过清理，id: " + image.getId());
            return false;
        }

        String objectKey = image.getObjectKey();
        if (StringUtils.hasText(objectKey)) {
            ossUtil.deleteOssObject(objectKey);
        } else {
            XxlJobHelper.log("图片 objectKey 为空，仅删除数据库记录，id: " + image.getId());
        }

        XxlJobHelper.log("图片清理成功，id: " + image.getId() + "，objectKey: " + objectKey);
        return true;
    }

    private int cleanupOssPrefix(String prefix, Instant expireBefore, int remainingDeletes,
                                 Integer deletedCount) {
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
                        ossUtil.deleteOssObject(objectKey);
                        deletedCount++;
                        remainingDeletes--;
                        XxlJobHelper.log("已删除 OSS 未登记图片，objectKey: " + objectKey);
                    } catch (Exception e) {
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
}
