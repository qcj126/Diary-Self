package diary.diaryai.impl;

import diary.common.entity.ai.po.AiTaskPO;
import diary.common.enums.aienum.AiTaskErrorCodeEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiTaskCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTaskRecoveryServiceImplTest {
    @Mock
    private DiaryAiMapper mapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AiTaskCommandService taskCommandService;

    private AiTaskRecoveryServiceImpl service;
    private AiTaskProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiTaskProperties();
        properties.getTask().setWaitingMaxRecoveryMessages(3);
        service = new AiTaskRecoveryServiceImpl(mapper, properties, eventPublisher, taskCommandService);
    }

    @Test
    void finalAutomaticRecoveryDoesNotTurnBrokerBacklogIntoTaskDeadLetter() {
        AiTaskPO task = AiTaskPO.builder()
                .id(7L)
                .userId(8L)
                .clientRequestId("request-7")
                .taskType(properties.getRocketmq().getTaskTag())
                .status("QUEUED")
                .attemptCount(0)
                .maxAttempts(3)
                .recoveryCount(2)
                .versionId(5)
                .build();
        when(mapper.countActiveTaskDispatchOutbox(7L)).thenReturn(0);
        when(mapper.recoverStaleWaiting(
                eq(7L), eq(5), eq(properties.getTask().getWaitingRecoverySeconds()), eq(3),
                eq(AiTaskErrorCodeEnum.DISPATCH_RECOVERY_EXHAUSTED.name()), any())).thenReturn(1);
        when(mapper.insertRetryTaskOutbox(any())).thenReturn(1);

        service.recoverWaiting(task);

        verify(mapper).insertRetryTaskOutbox(any());
        verify(taskCommandService, never()).deadLetterDispatchTask(any(), any());
    }
}
