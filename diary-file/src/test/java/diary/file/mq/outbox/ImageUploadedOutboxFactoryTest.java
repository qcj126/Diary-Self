package diary.file.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.exception.ConflictException;
import diary.file.entity.mq.po.FileMqOutboxPO;
import diary.file.entity.po.ImagePO;
import diary.file.property.FileMqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_EVENT_TYPE;
import static diary.file.consts.FileMqProtocolConst.IMAGE_UPLOADED_SCHEMA_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUploadedOutboxFactoryTest {
    private ImageUploadedOutboxFactory factory;

    @BeforeEach
    void setUp() {
        FileMqProperties properties = new FileMqProperties();
        properties.setEventTopic("diary-file-event");
        properties.setImageUploadedTag("IMAGE_UPLOADED");
        properties.setOutboxMaxRetries(5);
        factory = new ImageUploadedOutboxFactory(properties, new ObjectMapper());
    }

    @Test
    void createsFixedReplayablePayload() {
        FileMqOutboxPO outbox = factory.create(image());

        assertNotNull(outbox.getId());
        assertEquals(22L, outbox.getImageId());
        assertEquals(IMAGE_UPLOADED_EVENT_TYPE, outbox.getEventType());
        assertEquals(IMAGE_UPLOADED_SCHEMA_VERSION, outbox.getSchemaVersion());
        assertEquals(outbox.getEventId(), outbox.getMessageKey());
        assertEquals("NEW", outbox.getStatus());
        assertEquals(0, outbox.getRetryCount());
        assertEquals(5, outbox.getMaxRetries());
        factory.validateExisting(outbox, image());
    }

    @Test
    void rejectsExistingPayloadForDifferentObjectKey() {
        FileMqOutboxPO outbox = factory.create(image());
        ImagePO changed = image();
        changed.setObjectKey("images/original/11/22/changed.jpg");

        assertThrows(ConflictException.class, () -> factory.validateExisting(outbox, changed));
    }

    @Test
    void rejectsUnparseableExistingPayload() {
        FileMqOutboxPO outbox = factory.create(image());
        outbox.setPayload("not-json");

        assertThrows(ConflictException.class, () -> factory.validateExisting(outbox, image()));
    }

    private ImagePO image() {
        ImagePO image = new ImagePO();
        image.setId(22L);
        image.setUserId(11L);
        image.setObjectKey("images/original/11/2000/22-file.jpg");
        image.setOriginalName("file.jpg");
        return image;
    }
}
