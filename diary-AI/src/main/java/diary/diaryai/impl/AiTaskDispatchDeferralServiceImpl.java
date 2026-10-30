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
import diary.diaryai.service.AiTaskDispatchDeferralService;
import diary.diaryai.utils.MyUtil;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

import static diary.diaryai.consts.MqTaskConst.AI_TASK_AGGREGATE_TYPE;
import static diary.diaryai.consts.MqTaskConst.OUTBOX_EVENT_ID;
import static diary.diaryai.consts.MqTaskConst.OUTBOX_SCHEMA_VERSION;
import static diary.utils.commonutil.MyUtils.writeJson;

@Service
@RequiredArgsConstructor
public class AiTaskDispatchDeferralServiceImpl implements AiTaskDispatchDeferralService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;
    private final MyUtil myUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deferForGlobalModelPermit(AiTaskPO task, AiTaskErrorCodeEnum errorCode, String errorMessage, LocalDateTime nextRetryTime) {
        Objects.requireNonNull(task, "task 不能为空");
        Objects.requireNonNull(errorCode, "errorCode 不能为空");
        Objects.requireNonNull(nextRetryTime, "nextRetryTime 不能为空");

        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException("errorMessage 不能为空");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!nextRetryTime.isAfter(now)) {
            throw new IllegalArgumentException("nextRetryTime 必须是未来时间");
        }

        AiTaskProcessDto processDto = AiTaskProcessDto.builder()
                .taskId(task.getId())
                .userId(task.getUserId())
                .clientRequestId(task.getClientRequestId())
                .errorCode(errorCode.name())
                .errorMessage(errorMessage)
                .build();
        int changed = diaryAiMapper.markRetryWaitIfDispatchable(processDto);
        if (changed == 0) return false;

        String eventId = OUTBOX_EVENT_ID + MyUtils.getPrimaryKey();

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
                .tag(myUtil.getOutboxTag(task.getAiApplication()))
                .messageKey(String.valueOf(task.getId()))
                .payload(writeJson(retryMessage, "全局模型并发延迟重试消息序列化失败"))
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .status(OutboxStatusEnum.NEW.name())
                .retryCount(0)
                .maxRetries(properties.getRocketmq().getOutboxMaxRetries())
                .nextRetryTime(nextRetryTime)
                .createTime(now)
                .updateTime(now)
                .versionId(0)
                .build();
        if (diaryAiMapper.insertRetryTaskOutbox(outbox) != 1) {
            throw new IllegalStateException("全局模型并发延迟重试事件写入Outbox失败, taskId=" + task.getId());
        }
        return true;
    }
}
