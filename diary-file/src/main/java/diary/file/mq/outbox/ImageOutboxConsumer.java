package diary.file.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.file.exception.PermanentThumbnailProcessingException;
import diary.file.exception.RetryableThumbnailProcessingException;
import diary.file.mq.event.ImageUploadedEvent;
import diary.file.service.thumbnail.ImageThumbnailProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Objects;

import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_EVENT_TYPE;
import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_SCHEMA_VERSION;
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = "${diary.file.rocketmq.image-ready-consumer-group:diary-file-image-ready-consumer}",
        topic = "${diary.file.rocketmq.event-topic:diary-file-event}",
        tag = "${diary.file.rocketmq.image-uploaded-tag:IMAGE_UPLOADED}",
        sslEnabled = false
)
public class ImageOutboxConsumer implements RocketMQListener {
    private final ObjectMapper objectMapper;
    private final ImageThumbnailProcessService thumbnailProcessService;

    @Override
    public ConsumeResult consume(MessageView messageView) {
        ByteBuffer body = messageView.getBody();
        byte[] bytes = new byte[body.remaining()];
        body.get(bytes);

        final ImageUploadedEvent imageUploadedEvent;
        try {
            imageUploadedEvent = objectMapper.readValue(bytes, ImageUploadedEvent.class);
            validateMessage(imageUploadedEvent);
        } catch (Exception parseException) {
            /*
             * 协议或JSON永久非法时，重复投递相同字节没有恢复价值。ACK并记录messageId，
             * 避免坏消息无限占用消费重试；原始消息仍可通过Broker/应用日志定位。
             */
            log.error("非法IMAGE_UPLOADED消息，已停止重试, messageId={}",
                    messageView.getMessageId(), parseException);
            return ConsumeResult.SUCCESS;
        }

        try {
            thumbnailProcessService.process(imageUploadedEvent);
            return ConsumeResult.SUCCESS;
        } catch (PermanentThumbnailProcessingException permanentException) {
            /*处理服务已经尝试原子记录thumbnail FAILED和图片失败终态。*/
            log.error("缩略图永久处理失败，已停止重试, messageId={}, imageId={}",
                    messageView.getMessageId(), imageUploadedEvent.getImageId(), permanentException);
            return ConsumeResult.SUCCESS;
        } catch (RetryableThumbnailProcessingException retryableException) {
            log.warn("缩略图临时处理失败，等待RocketMQ重投, messageId={}, imageId={}",
                    messageView.getMessageId(), imageUploadedEvent.getImageId(), retryableException);
            return ConsumeResult.FAILURE;
        } catch (RuntimeException unexpectedException) {
            log.error("缩略图处理发生未分类异常，按可重试处理, messageId={}, imageId={}",
                    messageView.getMessageId(), imageUploadedEvent.getImageId(), unexpectedException);
            return ConsumeResult.FAILURE;
        }
    }

    // 再次校验消息，防止消息在传输过程中被篡改，或者其他功能的生产者误发消息到本消费者
    // 也可以防止某些非法调用
    private void validateMessage(ImageUploadedEvent message) {
        if (message == null
                || message.getEventId() == null
                || message.getEventId().isBlank()
                || message.getImageId() == null
                || message.getOccurredTime() == null
                || message.getObjectKey() == null
                || message.getObjectKey().isBlank()
                || message.getOriginalName() == null
                || message.getOriginalName().isBlank()
                || !Objects.equals(IMAGE_UPLOADED_SCHEMA_VERSION, message.getSchemaVersion())
                || !Objects.equals(IMAGE_UPLOADED_EVENT_TYPE, message.getEventType())) {
            throw new IllegalArgumentException("IMAGE_UPLOADED消息字段或协议版本不合法");
        }
    }
}
