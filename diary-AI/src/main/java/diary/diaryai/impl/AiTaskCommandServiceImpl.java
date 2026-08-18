package diary.diaryai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.common.enums.outbox.OutboxEventTypeEnum;
import diary.common.enums.outbox.OutboxStatusEnum;
import diary.common.util.MyUtil;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiTaskCommandService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.MDC;

import java.time.LocalDateTime;

import static diary.common.consts.AiTaskConst.OUTBOX_AGGREGATE_TYPE_ONE;
import static diary.common.consts.AiTaskConst.OUTBOX_EVENT_ID;
import static diary.common.consts.AiTaskConst.OUTBOX_SCHEMA_VERSION;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiTaskCommandServiceImpl implements AiTaskCommandService {
    private final DiaryAiMapper diaryAiMapper;
    private final ObjectMapper objectMapper;
    private final AiTaskProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiTaskPO createTaskAndOutbox(AiInvokeDTO request, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Long taskId = MyUtils.getPrimaryKey();
        // mq消息和outbox数据使用同一个eventId，保证事件的一致性
        // 不会出现mq消息属于另一个outbox数据的情况
        String eventId = OUTBOX_EVENT_ID + MyUtil.getPrimaryKey();
        String inputSnapshot = writeJson(request, "AI任务输入快照序列化失败");

        AiTaskPO task = AiTaskPO.builder()
                .id(taskId)
                .userId(userId)
                .clientRequestId(request.getClientRequestId())
                .taskType(properties.getRocketmq().getTaskTag())
                .status(AiTaskStatusEnum.PENDING.name())
                .inputSnapshot(inputSnapshot)
                .attemptCount(0)
                .maxAttempts(properties.getTask().getMaxAttempts())
                .createTime(now)
                .versionId(0)
                .build();

        AiTaskMessageDto message = AiTaskMessageDto.builder()
                .eventId(eventId)
                .taskId(taskId)
                .userId(userId)
                .clientRequestId(request.getClientRequestId())
                .taskType(properties.getRocketmq().getTaskTag())
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .occurTime(now)
                .traceId(MDC.get("traceId"))
                .build();

        MqOutboxPO outbox = MqOutboxPO.builder()
                .id(MyUtils.getPrimaryKey())
                .eventId(eventId)
                .aggregateType(OUTBOX_AGGREGATE_TYPE_ONE)
                .aggregateId(taskId)
                .eventType(OutboxEventTypeEnum.AI_TASK_CREATED.name())
                .topic(properties.getRocketmq().getTaskTopic())
                .tag(properties.getRocketmq().getTaskTag())
                .messageKey(taskId.toString())
                .payload(writeJson(message, "AI任务消息序列化失败"))
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .status(OutboxStatusEnum.NEW.name())
                .retryCount(0)
                .maxRetries(properties.getRocketmq().getOutboxMaxRetries())
                .nextRetryTime(now)
                .createTime(now)
                .updateTime(now)
                .versionId(0)
                .build();

        if (diaryAiMapper.insertAiTask(task) != 1) {
            throw new IllegalStateException("创建AI任务失败");
        }
        if (diaryAiMapper.insertOutbox(outbox) != 1) {
            throw new IllegalStateException("创建AI任务Outbox失败");
        }
        return task;
    }

    private String writeJson(Object value, String message) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(message, e);
        }
    }
}
