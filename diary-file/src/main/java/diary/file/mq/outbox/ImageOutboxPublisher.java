package diary.file.mq.outbox;

import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.mapper.FileMqOutboxMapper;
import diary.file.property.FileMqProperties;
import diary.file.service.filetransactionalservice.FileTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.core.RocketMQClientTemplate;
import org.apache.rocketmq.client.support.RocketMQHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ImageOutboxPublisher {
    private final FileMqOutboxMapper fileMqOutboxMapper;
    private final FileMqProperties fileMqProperties;
    private final FileTransactionalService fileTransactionalService;
    private final RocketMQClientTemplate rocketMQClientTemplate;

    @Scheduled(fixedDelayString = "${diary.file.rocketmq.publisher-interval-ms:10000}")
    public void publishUploadedImageMessages() {
        // 先尝试恢复超时的RETRY_WAIT状态的outbox消息
        List<FileMqOutboxPO> timeoutOutboxPOS = fileMqOutboxMapper.selectTimedOutSendingBatch(fileMqProperties.getOutboxSendingTimeoutSeconds(), fileMqProperties.getPublisherBatchSize());
        for (FileMqOutboxPO fileMqOutboxPO : timeoutOutboxPOS) {
            try {
                fileTransactionalService.recoverTimedOutForRetry(fileMqOutboxPO);
            } catch (Exception e) {
                log.error("Outbox失败状态更新失败: outboxId={}", fileMqOutboxPO.getId(), e);
            }
        }
        // 将NEW和RETRY_WAIT状态的outbox消息流转为SENDING
        List<FileMqOutboxPO> publishableOutboxPOS = fileMqOutboxMapper.selectPublishableOutboxBatch(fileMqProperties.getPublisherBatchSize());
        for (FileMqOutboxPO fileMqOutboxPO : publishableOutboxPOS) {
            try {
                if (!fileTransactionalService.claimForPublishing(fileMqOutboxPO.getId(), fileMqOutboxPO.getVersionId())) {
                    continue;
                }
            } catch (Exception e) {
                log.error("领取消息失败，outboxId={}, eventId={}", fileMqOutboxPO.getId(), fileMqOutboxPO.getEventId(), e);
                continue;
            }
            String msgId = null;
            try {
                fileMqOutboxPO.setVersionId(fileMqOutboxPO.getVersionId() + 1);
                Message<String> message = MessageBuilder
                        .withPayload(fileMqOutboxPO.getPayload())
                        .setHeader(RocketMQHeaders.KEYS, fileMqOutboxPO.getMessageKey())
                        .build();
                String destination = fileMqOutboxPO.getTopic() + ":" + fileMqOutboxPO.getTag();
                SendReceipt sendReceipt = rocketMQClientTemplate.syncSendNormalMessage(destination, message);
                msgId = sendReceipt.getMessageId().toString();
            } catch (RuntimeException e) {
                log.error("发送失败Broker 结果未知, outboxId={}, eventId={}", fileMqOutboxPO.getId(), fileMqOutboxPO.getEventId(), e);
            }
            if (msgId != null) {
                try {
                    boolean isMarkSent = fileTransactionalService.markSent(fileMqOutboxPO.getId(), fileMqOutboxPO.getVersionId(), msgId);
                    if (isMarkSent) {
                        log.info("数据库状态已从SENDING更新为SENT，outboxId={}, eventId={}", fileMqOutboxPO.getId(), fileMqOutboxPO.getEventId());
                    } else {
                        log.warn("确认SENT状态 CAS 未生效，记录可能已被超时恢复，outboxId={}, eventId={}", fileMqOutboxPO.getId(), fileMqOutboxPO.getEventId());
                    }
                } catch (RuntimeException e) {
                    log.error("发送成功但数据库异常, outboxId={}, eventId={}", fileMqOutboxPO.getId(), fileMqOutboxPO.getEventId(), e);
                }
            }
        }
    }
}
