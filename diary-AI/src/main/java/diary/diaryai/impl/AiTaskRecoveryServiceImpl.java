package diary.diaryai.impl;

import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.entity.mq.po.MqOutboxPO;
import diary.diaryai.enums.aienum.AiTaskErrorCodeEnum;
import diary.diaryai.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.enums.outbox.OutboxEventTypeEnum;
import diary.diaryai.enums.outbox.OutboxStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiTaskRecoveryService;
import diary.diaryai.service.AiTransactionService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static diary.diaryai.consts.MqTaskConst.AI_TASK_AGGREGATE_TYPE;
import static diary.diaryai.consts.MqTaskConst.OUTBOX_EVENT_ID;
import static diary.diaryai.consts.MqTaskConst.OUTBOX_SCHEMA_VERSION;
import static diary.utils.commonutil.MyUtils.writeJson;

@Service
@RequiredArgsConstructor
public class AiTaskRecoveryServiceImpl implements AiTaskRecoveryService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;
    private final AiTransactionService aiTransactionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recover(AiTaskPO task) {
        LocalDateTime now = LocalDateTime.now();

        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            aiTransactionService.failExhaustedTask(task, "RUNNING租约过期且尝试次数已耗尽");
            return;
        }

        AiTaskProcessDto retry = AiTaskProcessDto.builder()
                .taskId(task.getId())
                .versionId(task.getVersionId())
                .errorCode(AiTaskErrorCodeEnum.RETRYABLE_ERROR.name())
                .errorMessage("RUNNING租约过期，等待恢复")
                .build();

        if (diaryAiMapper.recoverExpiredRunning(retry) != 1) {
            // 任务已被迟到消息或其他恢复流程处理。
            return;
        }
        insertRetryTaskOutbox(task, now);
    }

    private void insertRetryTaskOutbox(AiTaskPO task, LocalDateTime now) {
        String eventId = OUTBOX_EVENT_ID + MyUtils.getPrimaryKey();

        String taskTagType = null;
//        switch (request.getAiApplication()) {
//            case 1 -> taskTagType = properties.getRocketmq().getTaskNutrientTag();
//            case 2 -> taskTagType = properties.getRocketmq().getTaskSentimentTag();
//        }

        AiTaskMessageDto retryMessage = AiTaskMessageDto.builder()
                .clientRequestId(task.getClientRequestId())
                .eventId(eventId)
                .taskId(task.getId())
                .userId(task.getUserId())
                .taskType(task.getTaskType())
                .eventType(OutboxEventTypeEnum.AI_TASK_RETRY.name())
                .taskStatus(AiTaskStatusEnum.RETRY_WAIT.name())
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .occurTime(now)
                .traceId(MDC.get("traceId"))
                .build();

        MqOutboxPO outbox = MqOutboxPO.builder()
                .id(MyUtils.getPrimaryKey())
                .eventId(eventId)
                .aggregateType(AI_TASK_AGGREGATE_TYPE)
                .aggregateId(task.getId())
                .eventType(OutboxEventTypeEnum.AI_TASK_RETRY.name())
                .topic(properties.getRocketmq().getTaskTopic())
                .tag(taskTagType)
                .messageKey(String.valueOf(task.getId()))
                .payload(writeJson(retryMessage, "AI任务恢复消息序列化失败"))
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .status(OutboxStatusEnum.NEW.name())
                .retryCount(0)
                .maxRetries(properties.getRocketmq().getOutboxMaxRetries())
                .nextRetryTime(now)
                .createTime(now)
                .updateTime(now)
                .versionId(0)
                .build();
        if (diaryAiMapper.insertRetryTaskOutbox(outbox) != 1) {
            throw new IllegalStateException("任务恢复事件写入Outbox失败, taskId=" + task.getId());
        }
    }

}
