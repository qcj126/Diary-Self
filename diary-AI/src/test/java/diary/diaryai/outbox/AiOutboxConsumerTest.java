package diary.diaryai.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.enums.aienum.AiTaskTypeEnum;
import diary.diaryai.executor.AiTaskExecutor;
import diary.diaryai.executor.AiTaskModelResolver;
import diary.diaryai.guard.AiTaskLeaseRenewer;
import diary.diaryai.guard.LocalAiConcurrencyGuard;
import diary.diaryai.guard.RedisModelPermitRenewer;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.redis.RedisModelPermitManager;
import diary.diaryai.service.AiTransactionService;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static diary.diaryai.consts.MqTaskConst.OUTBOX_SCHEMA_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiOutboxConsumerTest {
    private ObjectMapper objectMapper;
    private DiaryAiMapper mapper;
    private AiTaskExecutor executor;
    private AiTaskModelResolver modelResolver;
    private LocalAiConcurrencyGuard localGuard;
    private RedisModelPermitManager globalPermitManager;
    private RedisModelPermitRenewer globalPermitRenewer;
    private AiTaskLeaseRenewer taskLeaseRenewer;
    private AiTransactionService transactionService;
    private AiOutboxConsumer consumer;

    @BeforeEach
    void setUp() {
        AiTaskProperties properties = new AiTaskProperties();
        properties.getTask().setExecutionLeaseSeconds(330);
        objectMapper = mock(ObjectMapper.class);
        mapper = mock(DiaryAiMapper.class);
        executor = mock(AiTaskExecutor.class);
        modelResolver = mock(AiTaskModelResolver.class);
        localGuard = mock(LocalAiConcurrencyGuard.class);
        globalPermitManager = mock(RedisModelPermitManager.class);
        globalPermitRenewer = mock(RedisModelPermitRenewer.class);
        taskLeaseRenewer = mock(AiTaskLeaseRenewer.class);
        transactionService = mock(AiTransactionService.class);
        consumer = new AiOutboxConsumer(
                properties, objectMapper, mapper, executor, modelResolver, localGuard,
                globalPermitManager, globalPermitRenewer, taskLeaseRenewer, transactionService
        );
    }

    @Test
    void permanentlyInvalidMessageIsAcknowledged() throws Exception {
        AiTaskMessageDto invalid = validMessage();
        invalid.setSchemaVersion(-1);
        MessageView messageView = messageView();
        when(objectMapper.readValue(any(byte[].class), any(Class.class))).thenReturn(invalid);

        assertEquals(ConsumeResult.SUCCESS, consumer.consume(messageView));
        verify(mapper, never()).selectAiTaskByTaskId(any());
    }

    @Test
    void missingTaskIsAcknowledged() throws Exception {
        MessageView messageView = messageView();
        when(objectMapper.readValue(any(byte[].class), any(Class.class))).thenReturn(validMessage());
        when(mapper.selectAiTaskByTaskId(22L)).thenReturn(null);

        assertEquals(ConsumeResult.SUCCESS, consumer.consume(messageView));
        verify(localGuard, never()).tryAcquire();
    }

    @Test
    void globalCapacityFullUsesRocketMqRetryWithoutCreatingAnotherOutbox() throws Exception {
        MessageView messageView = messageView();
        AiTaskPO task = dispatchableTask();
        when(objectMapper.readValue(any(byte[].class), any(Class.class))).thenReturn(validMessage());
        when(mapper.selectAiTaskByTaskId(22L)).thenReturn(task);
        when(localGuard.tryAcquire()).thenReturn(true);
        when(modelResolver.resolve(task)).thenReturn(AIEnum.QWEN37PLUS);
        when(globalPermitManager.tryAcquire(any(), anyString())).thenReturn(false);

        assertEquals(ConsumeResult.FAILURE, consumer.consume(messageView));
        verify(mapper, never()).claimForExecution(any(AiTaskProcessDto.class));
        verify(localGuard).release();
    }

    @Test
    void claimedTaskExecutesWithBothLeasesAndReleasesPermits() throws Exception {
        MessageView messageView = messageView();
        AiTaskPO candidate = dispatchableTask();
        AiTaskPO claimed = dispatchableTask();
        claimed.setStatus(AiTaskStatusEnum.RUNNING.name());
        claimed.setWorkerId("worker-1");
        claimed.setVersionId(1);
        when(objectMapper.readValue(any(byte[].class), any(Class.class))).thenReturn(validMessage());
        when(mapper.selectAiTaskByTaskId(22L)).thenReturn(candidate, claimed);
        when(localGuard.tryAcquire()).thenReturn(true);
        when(modelResolver.resolve(candidate)).thenReturn(AIEnum.QWEN37PLUS);
        when(globalPermitManager.tryAcquire(any(), anyString())).thenReturn(true);
        when(mapper.claimForExecution(any(AiTaskProcessDto.class))).thenReturn(1);
        when(taskLeaseRenewer.start(22L, "worker-1", 1))
                .thenReturn(mock(AiTaskLeaseRenewer.LeaseRenewalHandle.class));
        when(globalPermitRenewer.start(any(), anyString()))
                .thenReturn(mock(RedisModelPermitRenewer.PermitRenewalHandle.class));

        assertEquals(ConsumeResult.SUCCESS, consumer.consume(messageView));
        verify(executor).execute(validMessage(), claimed);
        verify(globalPermitManager).release(any(), anyString());
        verify(localGuard).release();
    }

    private MessageView messageView() {
        MessageView messageView = mock(MessageView.class);
        when(messageView.getBody()).thenReturn(ByteBuffer.wrap(new byte[]{1}));
        return messageView;
    }

    private AiTaskMessageDto validMessage() {
        return AiTaskMessageDto.builder()
                .eventId("event-1")
                .taskId(22L)
                .userId(11L)
                .clientRequestId("request-1")
                .taskType(AiTaskTypeEnum.DIET_NUTRIENT_ANALYSIS.name())
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .build();
    }

    private AiTaskPO dispatchableTask() {
        return AiTaskPO.builder()
                .id(22L)
                .userId(11L)
                .clientRequestId("request-1")
                .status(AiTaskStatusEnum.QUEUED.name())
                .attemptCount(0)
                .maxAttempts(3)
                .versionId(0)
                .build();
    }
}
