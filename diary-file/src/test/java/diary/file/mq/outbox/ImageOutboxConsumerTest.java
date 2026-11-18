package diary.file.mq.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.file.exception.PermanentThumbnailProcessingException;
import diary.file.exception.RetryableThumbnailProcessingException;
import diary.file.mq.event.ImageUploadedEvent;
import diary.file.service.thumbnail.ImageThumbnailProcessService;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageOutboxConsumerTest {
    private ObjectMapper objectMapper;
    private ImageThumbnailProcessService processService;
    private ImageOutboxConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        processService = mock(ImageThumbnailProcessService.class);
        consumer = new ImageOutboxConsumer(objectMapper, processService);
    }

    @Test
    void successfulProcessingAcknowledgesMessage() throws Exception {
        MessageView message = message(objectMapper.writeValueAsBytes(validEvent()));

        assertEquals(ConsumeResult.SUCCESS, consumer.consume(message));
        verify(processService).process(any(ImageUploadedEvent.class));
    }

    @Test
    void retryableFailureRequestsRocketMqRedelivery() throws Exception {
        MessageView message = message(objectMapper.writeValueAsBytes(validEvent()));
        doThrow(new RetryableThumbnailProcessingException("network"))
                .when(processService).process(any());

        assertEquals(ConsumeResult.FAILURE, consumer.consume(message));
    }

    @Test
    void permanentFailureIsAcknowledgedAfterFailureStateWasRecorded() throws Exception {
        MessageView message = message(objectMapper.writeValueAsBytes(validEvent()));
        doThrow(new PermanentThumbnailProcessingException("bad image"))
                .when(processService).process(any());

        assertEquals(ConsumeResult.SUCCESS, consumer.consume(message));
    }

    @Test
    void invalidProtocolIsAcknowledgedWithoutCallingProcessor() {
        MessageView message = message("not-json".getBytes(StandardCharsets.UTF_8));

        assertEquals(ConsumeResult.SUCCESS, consumer.consume(message));
        verifyNoInteractions(processService);
    }

    private MessageView message(byte[] body) {
        MessageView message = mock(MessageView.class);
        when(message.getBody()).thenReturn(ByteBuffer.wrap(body));
        return message;
    }

    private ImageUploadedEvent validEvent() {
        return ImageUploadedEvent.builder()
                .eventId("event-1")
                .schemaVersion(1)
                .eventType("IMAGE_UPLOADED")
                .imageId(22L)
                .objectKey("images/original/11/2000/22-file.jpg")
                .originalName("file.jpg")
                .occurredTime(System.currentTimeMillis())
                .build();
    }
}
