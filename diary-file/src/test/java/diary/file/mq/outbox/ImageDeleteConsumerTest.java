package diary.file.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.file.mq.event.ImageDeletedEvent;
import diary.file.service.oss.OssStorageService;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageDeleteConsumerTest {
    @Test
    void deletesBothObjectsAndAcknowledges() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        OssStorageService storage = mock(OssStorageService.class);
        ImageDeleteConsumer consumer = new ImageDeleteConsumer(mapper, storage);
        ImageDeletedEvent event = event();

        assertEquals(ConsumeResult.SUCCESS, consumer.consume(message(mapper.writeValueAsBytes(event))));
        verify(storage).deleteObject(event.getOriginalObjectKey());
        verify(storage).deleteObject(event.getThumbnailObjectKey());
    }

    @Test
    void ossFailureRequestsRedelivery() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        OssStorageService storage = mock(OssStorageService.class);
        ImageDeleteConsumer consumer = new ImageDeleteConsumer(mapper, storage);
        ImageDeletedEvent event = event();
        doThrow(new RuntimeException("network")).when(storage).deleteObject(event.getOriginalObjectKey());

        assertEquals(ConsumeResult.FAILURE, consumer.consume(message(mapper.writeValueAsBytes(event))));
    }

    private MessageView message(byte[] bytes) {
        MessageView message = mock(MessageView.class);
        when(message.getBody()).thenReturn(ByteBuffer.wrap(bytes));
        return message;
    }

    private ImageDeletedEvent event() {
        return ImageDeletedEvent.builder().eventId("event-1").schemaVersion(1)
                .eventType("IMAGE_DELETED").imageId(22L)
                .originalObjectKey("images/original/11/22.jpg")
                .thumbnailObjectKey("images/thumbnail/11/22/v1.jpg")
                .occurredTime(System.currentTimeMillis()).build();
    }
}
