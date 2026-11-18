package diary.file.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.file.mq.event.ImageDeletedEvent;
import diary.file.service.oss.OssStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.annotation.RocketMQMessageListener;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Objects;

import static diary.file.consts.FileMqProtocolConst.IMAGE_DELETED_EVENT_TYPE;
import static diary.file.consts.FileMqProtocolConst.IMAGE_DELETED_SCHEMA_VERSION;

/** 幂等清理已逻辑删除图片的OSS原图和缩略图。 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        consumerGroup = "${diary.file.rocketmq.image-cleanup-consumer-group:diary-file-image-cleanup-consumer}",
        topic = "${diary.file.rocketmq.event-topic:diary-file-event}",
        tag = "${diary.file.rocketmq.image-deleted-tag:IMAGE_DELETED}",
        sslEnabled = false
)
public class ImageDeleteConsumer implements RocketMQListener {
    private final ObjectMapper objectMapper;
    private final OssStorageService ossStorageService;

    @Override
    public ConsumeResult consume(MessageView messageView) {
        final ImageDeletedEvent event;
        try {
            ByteBuffer body = messageView.getBody();
            byte[] bytes = new byte[body.remaining()];
            body.get(bytes);
            event = objectMapper.readValue(bytes, ImageDeletedEvent.class);
            validate(event);
        } catch (Exception exception) {
            log.error("非法IMAGE_DELETED消息，已停止重试, messageId={}", messageView.getMessageId(), exception);
            return ConsumeResult.SUCCESS;
        }

        try {
            ossStorageService.deleteObject(event.getOriginalObjectKey());
            if (event.getThumbnailObjectKey() != null && !event.getThumbnailObjectKey().isBlank()) {
                ossStorageService.deleteObject(event.getThumbnailObjectKey());
            }
            log.info("图片OSS对象清理完成, imageId={}, originalKey={}, thumbnailKey={}",
                    event.getImageId(), event.getOriginalObjectKey(), event.getThumbnailObjectKey());
            return ConsumeResult.SUCCESS;
        } catch (RuntimeException exception) {
            log.warn("图片OSS对象清理失败，等待RocketMQ重投, imageId={}", event.getImageId(), exception);
            return ConsumeResult.FAILURE;
        }
    }

    private void validate(ImageDeletedEvent event) {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()
                || event.getImageId() == null || event.getOccurredTime() == null
                || event.getOriginalObjectKey() == null || event.getOriginalObjectKey().isBlank()
                || !Objects.equals(event.getSchemaVersion(), IMAGE_DELETED_SCHEMA_VERSION)
                || !Objects.equals(event.getEventType(), IMAGE_DELETED_EVENT_TYPE)) {
            throw new IllegalArgumentException("IMAGE_DELETED消息字段或协议版本不合法");
        }
    }
}
