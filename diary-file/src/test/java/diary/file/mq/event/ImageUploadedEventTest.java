package diary.file.mq.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ImageUploadedEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeWithStableFieldNames() throws Exception {
        ImageUploadedEvent event = createEvent();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(event));

        assertEquals("file-evt-1001", json.get("eventId").asText());
        assertEquals(1, json.get("schemaVersion").asInt());
        assertEquals("IMAGE_UPLOADED", json.get("eventType").asText());
        assertEquals(1001L, json.get("imageId").asLong());
        assertEquals("images/2026/11/example.jpg", json.get("objectKey").asText());
        assertEquals("example.jpg", json.get("originalName").asText());
        assertEquals(1793865600000L, json.get("occurredTime").asLong());
        assertFalse(json.has("occurredAt"));
    }

    @Test
    void shouldRoundTripAllFields() throws Exception {
        ImageUploadedEvent expected = createEvent();

        String json = objectMapper.writeValueAsString(expected);
        ImageUploadedEvent actual = objectMapper.readValue(json, ImageUploadedEvent.class);

        assertEquals(expected, actual);
    }

    @Test
    void shouldIgnoreUnknownFieldsWhenDeserializing() throws Exception {
        String json = """
                {
                  "eventId": "file-evt-1001",
                  "schemaVersion": 1,
                  "eventType": "IMAGE_UPLOADED",
                  "imageId": 1001,
                  "objectKey": "images/2026/11/example.jpg",
                  "originalName": "example.jpg",
                  "occurredTime": 1793865600000,
                  "futureField": "future-value"
                }
                """;

        ImageUploadedEvent event = objectMapper.readValue(json, ImageUploadedEvent.class);

        assertEquals(createEvent(), event);
    }

    private ImageUploadedEvent createEvent() {
        return ImageUploadedEvent.builder()
                .eventId("file-evt-1001")
                .schemaVersion(1)
                .eventType("IMAGE_UPLOADED")
                .imageId(1001L)
                .objectKey("images/2026/11/example.jpg")
                .originalName("example.jpg")
                .occurredTime(1793865600000L)
                .build();
    }
}
