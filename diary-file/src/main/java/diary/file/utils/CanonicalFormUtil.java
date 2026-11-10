package diary.file.utils;

import diary.file.mq.event.ImageUploadedEvent;

import java.util.Map;
import java.util.TreeMap;

public class CanonicalFormUtil {
    public static Map<String, Object> canonicalUploadedEventForm(ImageUploadedEvent uploadedEvent) {
        Map<String, Object> value = new TreeMap<>();
        value.put("eventId", uploadedEvent.getEventId());
        value.put("schemaVersion", uploadedEvent.getSchemaVersion());
        value.put("eventType", uploadedEvent.getEventType());
        value.put("imageId", uploadedEvent.getImageId());
        value.put("objectKey", uploadedEvent.getObjectKey());
        value.put("originalName", uploadedEvent.getOriginalName());
        value.put("occurredTime", uploadedEvent.getOccurredTime());
        return value;
    }
}
