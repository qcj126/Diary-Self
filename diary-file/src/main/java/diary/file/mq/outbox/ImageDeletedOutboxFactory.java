package diary.file.mq.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.exception.ConflictException;
import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.mq.event.ImageDeletedEvent;
import diary.file.property.FileMqProperties;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;

import static diary.file.consts.FileMqProtocolConst.EVENT_ID_PREFIX;
import static diary.file.consts.FileMqProtocolConst.IMAGE_DELETED_EVENT_TYPE;
import static diary.file.consts.FileMqProtocolConst.IMAGE_DELETED_SCHEMA_VERSION;
import static diary.file.enums.FileMqOutboxStatusEnum.NEW;
import static diary.utils.common.MyUtils.writeJson;

/** 构造一次性持久化、后续原样重放的IMAGE_DELETED Outbox。 */
@Component
@RequiredArgsConstructor
public class ImageDeletedOutboxFactory {
    private final FileMqProperties properties;
    private final ObjectMapper objectMapper;

    public FileMqOutboxPO create(Long imageId, String originalObjectKey, String thumbnailObjectKey) {
        MyUtils.check().notNull(imageId, "imageId").notEmpty(originalObjectKey, "originalObjectKey");
        long id = MyUtils.getPrimaryKey();
        String eventId = EVENT_ID_PREFIX + id;
        LocalDateTime now = LocalDateTime.now();
        ImageDeletedEvent event = ImageDeletedEvent.builder()
                .eventId(eventId)
                .schemaVersion(IMAGE_DELETED_SCHEMA_VERSION)
                .eventType(IMAGE_DELETED_EVENT_TYPE)
                .imageId(imageId)
                .originalObjectKey(originalObjectKey)
                .thumbnailObjectKey(thumbnailObjectKey)
                .occurredTime(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .build();
        Map<String, Object> payload = new TreeMap<>();
        payload.put("eventId", event.getEventId());
        payload.put("schemaVersion", event.getSchemaVersion());
        payload.put("eventType", event.getEventType());
        payload.put("imageId", event.getImageId());
        payload.put("originalObjectKey", event.getOriginalObjectKey());
        payload.put("thumbnailObjectKey", event.getThumbnailObjectKey());
        payload.put("occurredTime", event.getOccurredTime());
        return FileMqOutboxPO.builder()
                .id(id).eventId(eventId).imageId(imageId).eventType(IMAGE_DELETED_EVENT_TYPE)
                .topic(properties.getEventTopic()).tag(properties.getImageDeletedTag())
                .messageKey(eventId).payload(writeJson(payload, "图片删除消息序列化失败"))
                .schemaVersion(IMAGE_DELETED_SCHEMA_VERSION).status(NEW.name())
                .retryCount(0).maxRetries(properties.getOutboxMaxRetries())
                .nextRetryTime(now).versionId(0).build();
    }

    public void validateExisting(
            FileMqOutboxPO outbox,
            Long imageId,
            String originalObjectKey,
            String thumbnailObjectKey
    ) {
        if (outbox == null || !Objects.equals(outbox.getImageId(), imageId)
                || !Objects.equals(outbox.getEventType(), IMAGE_DELETED_EVENT_TYPE)
                || !Objects.equals(outbox.getTopic(), properties.getEventTopic())
                || !Objects.equals(outbox.getTag(), properties.getImageDeletedTag())
                || !Objects.equals(outbox.getSchemaVersion(), IMAGE_DELETED_SCHEMA_VERSION)
                || outbox.getPayload() == null || outbox.getPayload().isBlank()) {
            throw new ConflictException("已有IMAGE_DELETED Outbox元数据冲突");
        }
        try {
            ImageDeletedEvent event = objectMapper.readValue(outbox.getPayload(), ImageDeletedEvent.class);
            if (!Objects.equals(event.getImageId(), imageId)
                    || !Objects.equals(event.getOriginalObjectKey(), originalObjectKey)
                    || !Objects.equals(event.getThumbnailObjectKey(), thumbnailObjectKey)
                    || !Objects.equals(event.getEventId(), outbox.getEventId())) {
                throw new ConflictException("已有IMAGE_DELETED Outbox payload冲突");
            }
        } catch (JsonProcessingException exception) {
            throw new ConflictException("已有IMAGE_DELETED Outbox payload无法解析");
        }
    }
}
