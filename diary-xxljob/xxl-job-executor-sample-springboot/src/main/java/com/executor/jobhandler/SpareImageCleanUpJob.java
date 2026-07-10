package com.executor.jobhandler;

import com.executor.service.ImageCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import diary.common.entity.xxlJob.ImageCleanUpResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpareImageCleanUpJob {

    private final ImageCleanupService imageCleanupService;

    public SpareImageCleanUpJob(ImageCleanupService imageCleanupService) {
        this.imageCleanupService = imageCleanupService;
    }

    @XxlJob("spareImageCleanUpHandler")
    public void unusedImageCleanupHandler() {
        XxlJobHelper.log("开始清理未被业务表关联的图片");

        ImageCleanUpResultDTO result = imageCleanupService.cleanupUnreferencedImages();

        String message = String.format(
                "未关联图片清理完成，扫描: %d，删除成功: %d，删除失败: %d",
                result.getScannedCount(),
                result.getDeletedCount(),
                result.getFailedCount()
        );
        log.info(message);
        XxlJobHelper.log(message);

        if (result.getFailedCount() > 0) {
            XxlJobHelper.handleFail(message);
        }
    }
}
