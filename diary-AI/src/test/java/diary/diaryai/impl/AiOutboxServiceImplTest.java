package diary.diaryai.impl;

import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.enums.outbox.OutboxEventTypeEnum;
import diary.common.enums.outbox.OutboxStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiTaskCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiOutboxServiceImplTest {
    @Mock
    private DiaryAiMapper mapper;
    @Mock
    private AiTaskCommandService taskCommandService;

    private AiOutboxServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AiOutboxServiceImpl(mapper, new AiTaskProperties(), taskCommandService);
    }

    @Test
    void sendingTimeoutConsumesRetryBudget() {
        MqOutboxPO outbox = outbox(1, 3);
        when(mapper.markOutboxRetry(
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(4),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(1);

        service.recoverSendingTimeout(outbox);

        verify(mapper).markOutboxRetry(
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(4),
                anyLong(),
                contains("SENDING_TIMEOUT"));
        verify(mapper, never()).markOutboxDead(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void sendingTimeoutDeadLettersDispatchAfterRetryBudgetIsExhausted() {
        MqOutboxPO outbox = outbox(3, 3);
        AiTaskPO task = AiTaskPO.builder().id(99L).build();
        when(mapper.markOutboxDead(11L, 4, "SENDING_TIMEOUT_RECOVERED")).thenReturn(1);
        when(mapper.selectAiTaskByTaskId(99L)).thenReturn(task);
        when(taskCommandService.deadLetterDispatchTask(task,
                "任务消息Outbox重试耗尽, outboxId=11")).thenReturn(true);

        service.recoverSendingTimeout(outbox);

        verify(mapper).markOutboxDead(11L, 4, "SENDING_TIMEOUT_RECOVERED");
        verify(taskCommandService).deadLetterDispatchTask(task,
                "任务消息Outbox重试耗尽, outboxId=11");
    }

    @Test
    void sentRetryMessageMovesWaitingTaskBackToQueued() {
        MqOutboxPO outbox = outbox(1, 3);
        outbox.setEventType(OutboxEventTypeEnum.AI_TASK_RETRY.name());
        when(mapper.markOutboxSent(11L, 4, "broker-1")).thenReturn(1);

        service.confirmSent(outbox, "broker-1");

        verify(mapper).markQueuedByTaskIdIfWaiting(99L);
    }

    private MqOutboxPO outbox(int retryCount, int maxRetries) {
        return MqOutboxPO.builder()
                .id(11L)
                .aggregateId(99L)
                .eventType(OutboxEventTypeEnum.AI_TASK_CREATED.name())
                .status(OutboxStatusEnum.SENDING.name())
                .retryCount(retryCount)
                .maxRetries(maxRetries)
                .versionId(4)
                .build();
    }
}
