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
        if (cleanBatchSize != cleanCnt) {
            log.warn("成功删除Outbox消息 数据量：{}条, 期望删除数据量：{}条", cleanCnt, cleanBatchSize);
        } else {
            log.info("成功删除Outbox消息 {}条", cleanCnt);
        }
    }
}
