package diary.file.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.file.entity.po.ImagePO;
import diary.file.enums.ImageTransitionResultEnum;
import diary.file.mapper.ImageMapper;
import diary.file.mq.event.ImageUploadedEvent;
import diary.file.service.ImageStatusService;
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
import static diary.file.enums.ImageTransitionResultEnum.REJECTED;

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
//    private final FileMqOutboxMapper fileMqOutboxMapper;
    private final ImageMapper imageMapper;
    private final ImageStatusService imageStatusService;

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
             * 以前只捕获 IOException，schemaVersion、taskId 等协议错误没有明确分类，可能直接穿透 Listener。
             * 这类消息无法定位到可靠任务，返回 FAILURE 让 RocketMQ 按消费策略有限重试并最终进入 DLQ。
             */
            log.error("Invalid AI task message, messageId={}", messageView.getMessageId(), parseException);
            return ConsumeResult.FAILURE;
        }

        ImagePO imagePO;
        try {
            // 通过imageId查询图片信息
            imagePO = imageMapper.selectImageStatusById(imageUploadedEvent.getImageId());
            if (imagePO == null) {
                log.error("消息中imageId在传输中已被更改，当前imageId = {}", imageUploadedEvent.getImageId());
                return ConsumeResult.FAILURE;
            }
            if (!imagePO.getObjectKey().equals(imageUploadedEvent.getObjectKey())) {
                log.error("消息中objectKey在传输中已被更改，当前imageId = {}, 当前objectKey = {}, 期望objectKey = {}",
                        imageUploadedEvent.getImageId(), imagePO.getObjectKey(), imageUploadedEvent.getObjectKey());
                return ConsumeResult.FAILURE;
            }
        } catch (Exception e) {
            log.error("查询图片数据失败，imageId = {}", imageUploadedEvent.getImageId(), e);
            return ConsumeResult.FAILURE;
        }

        try {
            ImageTransitionResultEnum transitionResultEnum = imageStatusService.markReady(imagePO.getId());
            if (transitionResultEnum == REJECTED) {
                log.warn("状态拒绝迁移，imageId为 = {}", imagePO.getId());
            }
            return ConsumeResult.SUCCESS;
        } catch (Exception e) {
            log.error("流转READY状态出现异常，imageId = {}", imagePO.getId(), e);
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
            throw new IllegalArgumentException("AI任务消息字段或协议版本不合法");
        }
    }
}
