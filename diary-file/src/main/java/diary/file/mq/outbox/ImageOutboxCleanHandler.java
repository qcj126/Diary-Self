package diary.file.mq.outbox;

import diary.file.mapper.FileMqOutboxMapper;
import diary.file.property.FileMqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageOutboxCleanHandler {
    private final FileMqOutboxMapper fileMqOutboxMapper;
    private final FileMqProperties fileMqProperties;
    @Scheduled(fixedDelayString = "${diary.file.rocketmq.outbox-clean-ms:250000}")
    public void cleanImageOutboxData() {
        Integer sentRetentionDays = fileMqProperties.getSentRetentionDays();
        Integer cleanBatchSize = fileMqProperties.getCleanBatchSize();
        int cleanCnt = fileMqOutboxMapper.cleanSentOutboxData(sentRetentionDays, cleanBatchSize);
        if (cleanCnt > 0) {
            log.info("已清理过期SENT Outbox, count={}, batchLimit={}", cleanCnt, cleanBatchSize);
        } else {
            log.debug("本轮没有需要清理的SENT Outbox");
        }
    }
}
