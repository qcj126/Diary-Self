package diary.file.mq.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.exception.ConflictException;
import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.entity.po.ImagePO;
import diary.file.mq.event.ImageUploadedEvent;
import diary.file.property.FileMqProperties;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

import static diary.file.consts.FileMqProtocolConst.EVENT_ID_PREFIX;
import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_EVENT_TYPE;
import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_SCHEMA_VERSION;
import static diary.file.enums.FileMqOutboxStatusEnum.NEW;
import static diary.file.utils.CanonicalFormUtil.canonicalUploadedEventForm;
import static diary.utils.common.MyUtils.writeJson;

/**
 * 构造并核验 IMAGE_UPLOADED Outbox 数据。
 *
 * <p>该组件不访问数据库、不发送 RocketMQ，也不包含事务。事件 JSON 只在首次落库时生成，
 * Outbox 后续重试必须复用数据库中已经持久化的 payload。</p>
 */
@Component
@RequiredArgsConstructor
public class ImageUploadedOutboxFactory {
    private final FileMqProperties properties;
    private final ObjectMapper objectMapper;

    public FileMqOutboxPO create(ImagePO image) {
        validateImage(image);

        long outboxId = MyUtils.getPrimaryKey();
        String eventId = EVENT_ID_PREFIX + outboxId;
        LocalDateTime now = LocalDateTime.now();
        long occurredTime = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        ImageUploadedEvent event = ImageUploadedEvent.builder()
                .eventId(eventId)
                .schemaVersion(IMAGE_UPLOADED_SCHEMA_VERSION)
                .eventType(IMAGE_UPLOADED_EVENT_TYPE)
                .imageId(image.getId())
                .objectKey(image.getObjectKey())
                .originalName(image.getOriginalName())
                .occurredTime(occurredTime)
                .build();

        return FileMqOutboxPO.builder()
                .id(outboxId)
                .eventId(eventId)
                .imageId(image.getId())
                .eventType(IMAGE_UPLOADED_EVENT_TYPE)
                .topic(properties.getEventTopic())
                .tag(properties.getImageUploadedTag())
                .messageKey(eventId)
                .payload(writeJson(canonicalUploadedEventForm(event), "图片上传消息序列化失败"))
                .schemaVersion(IMAGE_UPLOADED_SCHEMA_VERSION)
                .status(NEW.name())
                .retryCount(0)
                .maxRetries(properties.getOutboxMaxRetries())
                .nextRetryTime(now)
                .versionId(0)
                .build();
    }

    /**
     * 已有唯一事件不能只凭“数据库中有一行”就视为幂等成功，必须确认列值和固定 payload
     * 仍然描述同一张图片，防止历史脏数据被静默接受。
     */
    public void validateExisting(FileMqOutboxPO outbox, ImagePO image) {
        validateImage(image);
        if (outbox == null
                || !Objects.equals(outbox.getImageId(), image.getId())
                || !Objects.equals(outbox.getEventType(), IMAGE_UPLOADED_EVENT_TYPE)
                || !Objects.equals(outbox.getTopic(), properties.getEventTopic())
                || !Objects.equals(outbox.getTag(), properties.getImageUploadedTag())
                || !Objects.equals(outbox.getSchemaVersion(), IMAGE_UPLOADED_SCHEMA_VERSION)
                || outbox.getEventId() == null
                || outbox.getEventId().isBlank()
                || !Objects.equals(outbox.getMessageKey(), outbox.getEventId())
                || outbox.getPayload() == null
                || outbox.getPayload().isBlank()) {
            throw new ConflictException("已有IMAGE_UPLOADED Outbox元数据与图片不一致");
        }

        final ImageUploadedEvent event;
        try {
            event = objectMapper.readValue(outbox.getPayload(), ImageUploadedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new ConflictException("已有IMAGE_UPLOADED Outbox payload无法解析");
        }

        if (!Objects.equals(event.getEventId(), outbox.getEventId())
                || !Objects.equals(event.getSchemaVersion(), IMAGE_UPLOADED_SCHEMA_VERSION)
                || !Objects.equals(event.getEventType(), IMAGE_UPLOADED_EVENT_TYPE)
                || !Objects.equals(event.getImageId(), image.getId())
                || !Objects.equals(event.getObjectKey(), image.getObjectKey())
                || !Objects.equals(event.getOriginalName(), image.getOriginalName())
                || event.getOccurredTime() == null) {
            throw new ConflictException("已有IMAGE_UPLOADED Outbox payload与图片不一致");
        }
    }

    private void validateImage(ImagePO image) {
        MyUtils.check()
                .notNull(image, "image")
                .notNull(image.getId(), "imageId")
                .notEmpty(image.getObjectKey(), "objectKey")
                .notEmpty(image.getOriginalName(), "originalName");
    }
}
