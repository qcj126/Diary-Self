package com.xxl.job.executor.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.executor.service.ImageCleanupService;
import com.xxl.job.executor.service.ImageCleanupService.ImageCleanupResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UnusedImageCleanupJob {

    private final ImageCleanupService imageCleanupService;

    public UnusedImageCleanupJob(ImageCleanupService imageCleanupService) {
        this.imageCleanupService = imageCleanupService;
    }

    @XxlJob("unusedImageCleanupHandler")
    public void unusedImageCleanupHandler() {
        XxlJobHelper.log("开始清理未被业务表关联的图片");

        ImageCleanupResult result = imageCleanupService.cleanupUnreferencedImages();

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
