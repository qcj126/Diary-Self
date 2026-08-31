package diary.diaryai.impl;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.dto.AiTaskEventDto;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.enums.aienum.AiTaskErrorCodeEnum;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.common.enums.outbox.OutboxEventTypeEnum;
import diary.common.enums.outbox.OutboxStatusEnum;

import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiTaskCommandService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Map;

import static diary.common.consts.AiTaskConst.*;
import static diary.utils.commonutil.MyUtils.writeJson;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiTaskCommandServiceImpl implements AiTaskCommandService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiTaskPO createTaskAndOutbox(AiInvokeDTO request, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Long taskId = MyUtils.getPrimaryKey();
        // mq消息和outbox数据使用同一个eventId，保证事件的一致性
        // 不会出现mq消息属于另一个outbox数据的情况
        String eventId = OUTBOX_EVENT_ID + MyUtils.getPrimaryKey();
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
                .updateTime(now)
                .versionId(0)
                .build();

        AiTaskMessageDto message = AiTaskMessageDto.builder()
                .eventId(eventId)
                .taskId(taskId)
                .userId(userId)
                .clientRequestId(request.getClientRequestId())
                .taskType(properties.getRocketmq().getTaskTag())
                .eventType(OutboxEventTypeEnum.AI_TASK_CREATED.name())
                .taskStatus(AiTaskStatusEnum.PENDING.name())
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .occurTime(now)
                .traceId(MDC.get("traceId"))
                .build();

        MqOutboxPO outbox = MqOutboxPO.builder()
                .id(MyUtils.getPrimaryKey())
                .eventId(eventId)
                .aggregateType(AI_TASK_AGGREGATE_TYPE)
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

    /**
     * @param model AI模型
     * @param result AI结果
     * @param temperature 温度参数
     * @param userId 用户ID
     * @param workerId 当前任务抢占者；用于阻止旧 Worker 提交结果
     * @param versionId Consumer 抢占成功后的乐观锁版本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processData(Long taskId, Object data, String model, Map<String, String> result, Double temperature, Long userId, String workerId, Integer versionId) {
        String eventId = OUTBOX_EVENT_ID + MyUtils.getPrimaryKey();
        LocalDateTime now = LocalDateTime.now();

        AiInvokeDTO aiInvokeDTO = (AiInvokeDTO) data;
        AiInfoPO aiInfoPO = AiInfoPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(userId)
                .temperature(String.valueOf(temperature))
                .model(model)
                .aiType(aiInvokeDTO.getAiType())
                .aiApplication(aiInvokeDTO.getAiApplication())
                .build();
        AiNutrientPO aiNutrientPO = AiNutrientPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(userId)
                .universalId(aiInvokeDTO.getUniversalId())
                .aiInfoId(aiInfoPO.getId())
                .calory(result.get("卡路里"))
                .protein(result.get("蛋白质"))
                .fat(result.get("脂肪"))
                .carbohydrate(result.get("碳水化合物"))
                .sugar(result.get("糖"))
                .sodium(result.get("钠"))
                .flag(aiInvokeDTO.getFlag())
                .aiTaskId(taskId)
                .build();
        AiTaskProcessDto aiTaskProcessDto = AiTaskProcessDto.builder()
                .taskId(taskId)
                .userId(userId)
                .clientRequestId(aiInvokeDTO.getClientRequestId())
                .workerId(workerId)
                .versionId(versionId)
                .aiInfoId(aiInfoPO.getId())
                .finishTime(LocalDateTime.now())
                .build();

        AiTaskEventDto message = AiTaskEventDto.builder()
                .eventId(eventId)
                .eventType(OutboxEventTypeEnum.AI_COMPLETED.name())
                .taskId(taskId)
                .userId(userId)
                .taskStatus(AiTaskStatusEnum.SUCCESS.name())
                .resultId(aiInfoPO.getId())
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .occurTime(now)
                .traceId(MDC.get("traceId"))
                .build();

        MqOutboxPO mqOutboxPO = MqOutboxPO.builder()
                .id(MyUtils.getPrimaryKey())
                .eventId(eventId)
                .aggregateType(AI_TASK_AGGREGATE_TYPE)
                .aggregateId(taskId)
                .eventType(OutboxEventTypeEnum.AI_COMPLETED.name())
                .topic(properties.getRocketmq().getEventTopic())
                .tag(properties.getRocketmq().getCompletedTag())
                .messageKey(String.valueOf(taskId))
                .payload(writeJson(message, "AI任务消息序列化失败"))
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .status(OutboxStatusEnum.NEW.name())
                .retryCount(0)
                .maxRetries(properties.getRocketmq().getOutboxMaxRetries())
                .nextRetryTime(now)
                .brokerMessageId(null)
                .lastError(null)
                .sentTime(null)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .versionId(0)
                .build();
        int aiInfoCnt = diaryAiMapper.insertAiInfo(aiInfoPO);
        int aiNutrientCnt = diaryAiMapper.insertAiNutrient(aiNutrientPO);
        int aiTaskCnt = diaryAiMapper.markSuccessIfOwned(aiTaskProcessDto);
        int mqOutboxCnt = diaryAiMapper.insertOutbox(mqOutboxPO);
        /*
         * 以前任意一步失败后，会在同一个事务里把任务改回 PENDING 再抛异常；但抛异常会让该更新一起回滚，
         * 而且执行失败也不应回到“消息尚未发送”的 PENDING。现在四步必须都恰好影响一行，否则直接抛出，
         * 让 AiInfo、AiNutrient 和 SUCCESS 状态整体回滚，再由 Consumer 在事务外写 RETRY_WAIT/FAILED。
         * SUCCESS 更新还校验 workerId + versionId，旧 Worker 已失去租约时不会提交重复结果。
         */
        if (aiInfoCnt != 1 || aiNutrientCnt != 1 || aiTaskCnt != 1 || mqOutboxCnt != 1) {
            throw new IllegalStateException(
                    "AI结果事务提交失败: aiInfo=" + aiInfoCnt
                            + ", aiNutrient=" + aiNutrientCnt
                            + ", aiTask=" + aiTaskCnt
                            + ", mqOutbox=" + mqOutboxCnt
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumeResult handleExecutionFailure(AiTaskMessageDto message, AiTaskPO claimedTask, String workerId, Exception executionException) {
        LocalDateTime now = LocalDateTime.now();

        boolean permanentError = executionException instanceof IllegalArgumentException;
        boolean attemptsExhausted = claimedTask.getAttemptCount() >= claimedTask.getMaxAttempts();
        String errorMessage = truncateErrorMessage(executionException.getMessage());

        AiTaskProcessDto failureRequest = AiTaskProcessDto.builder()
                .taskId(message.getTaskId())
                .userId(message.getUserId())
                .clientRequestId(message.getClientRequestId())
                .workerId(workerId)
                .versionId(claimedTask.getVersionId())
                .errorCode(
                        permanentError ? AiTaskErrorCodeEnum.PERMANENT_ERROR.name() :
                                attemptsExhausted ? AiTaskErrorCodeEnum.RETRY_EXHAUSTED.name() : AiTaskErrorCodeEnum.RETRYABLE_ERROR.name())
                .errorMessage(errorMessage)
                .finishTime(now)
                .build();

        if (permanentError || attemptsExhausted) {
            /*
             * 改前：虽然 FAILED 与 Outbox 在同一事务，但 Outbox payload 直接复用了原任务消息，导致
             * mq_outbox.event_id 与 payload.eventId 不一致，失败消息的 taskType/Tag 也仍是任务或完成事件。
             * 改后：FAILED 条件更新成功后，统一通过 appendTerminalEvent 创建全新的失败事件消息。
             * 效果：数据库事件 ID、payload 事件 ID、eventType 和 AI_FAILED Tag 完全一致，下游可可靠去重与路由。
             */
            int failed = diaryAiMapper.markFailedIfOwned(failureRequest);
            if (failed != 1) {
                return handleOwnershipLost(message.getTaskId());
            }

            log.error("AI task failed permanently, taskId={}, attemptCount={}",
                    message.getTaskId(), claimedTask.getAttemptCount(), executionException);
            appendTerminalEvent(
                    claimedTask,
                    AiTaskStatusEnum.FAILED.name(),
                    failureRequest.getErrorCode(),
                    failureRequest.getErrorMessage()
            );
            log.info("AI任务失败并写入outbox, taskId={}", message.getTaskId());
            return ConsumeResult.SUCCESS;
        }

        int retryWaiting = diaryAiMapper.markRetryWaitIfOwned(failureRequest);
        log.warn("AI task will be retried, taskId={}, attemptCount={}",
                message.getTaskId(), claimedTask.getAttemptCount(), executionException);
        return retryWaiting == 1 ? ConsumeResult.FAILURE : handleOwnershipLost(message.getTaskId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean failExhaustedTask(AiTaskPO task, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        AiTaskProcessDto failed = AiTaskProcessDto.builder()
                .taskId(task.getId())
                .userId(task.getUserId())
                .clientRequestId(task.getClientRequestId())
                .versionId(task.getVersionId())
                .finishTime(now)
                .errorCode(AiTaskErrorCodeEnum.RETRY_EXHAUSTED.name())
                .errorMessage(truncateErrorMessage(errorMessage))
                .build();

        /*
         * 改前：Consumer 在“次数耗尽”分支只把 task 改成 FAILED 后就 ACK，Recovery Job 因此再也扫描不到它，
         * 最终不会产生 AI_FAILED Outbox。
         * 改后：FAILED 状态迁移和失败事件 Outbox 统一放进本事务；任一步失败都会整体回滚。
         * 效果：无论 Consumer 还是 Recovery Job 先处理到任务，终态与终态事件都不会只成功一半。
         */
        if (diaryAiMapper.markFailedIfAttemptsExhausted(failed) != 1) {
            return false;
        }
        appendTerminalEvent(task, AiTaskStatusEnum.FAILED.name(), failed.getErrorCode(), failed.getErrorMessage());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deadLetterDispatchTask(AiTaskPO task, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        AiTaskProcessDto deadLetter = AiTaskProcessDto.builder()
                .taskId(task.getId())
                .userId(task.getUserId())
                .clientRequestId(task.getClientRequestId())
                .versionId(task.getVersionId())
                .finishTime(now)
                .errorCode(AiTaskErrorCodeEnum.OUTBOX_SEND_FAILED.name())
                .errorMessage(truncateErrorMessage(errorMessage))
                .build();
        if (diaryAiMapper.markDeadLetterIfDispatchable(deadLetter) != 1) {
            return false;
        }
        appendTerminalEvent(task, AiTaskStatusEnum.DEAD_LETTER.name(), deadLetter.getErrorCode(), deadLetter.getErrorMessage());
        return true;
    }

    private void appendTerminalEvent(AiTaskPO task, String taskStatus, String errorCode, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        String eventId = OUTBOX_EVENT_ID + MyUtils.getPrimaryKey();
        AiTaskEventDto terminalMessage = AiTaskEventDto.builder()
                .eventId(eventId)
                .eventType(OutboxEventTypeEnum.AI_FAILED.name())
                .taskId(task.getId())
                .userId(task.getUserId())
                .taskStatus(taskStatus)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .occurTime(now)
                .traceId(MDC.get("traceId"))
                .build();

        MqOutboxPO outbox = MqOutboxPO.builder()
                .id(MyUtils.getPrimaryKey())
                .eventId(eventId)
                .aggregateType(AI_TASK_AGGREGATE_TYPE)
                .aggregateId(task.getId())
                .eventType(OutboxEventTypeEnum.AI_FAILED.name())
                .topic(properties.getRocketmq().getEventTopic())
                .tag(properties.getRocketmq().getFailedTag())
                .messageKey(String.valueOf(task.getId()))
                .payload(writeJson(terminalMessage, "AI终态失败消息序列化失败"))
                .schemaVersion(OUTBOX_SCHEMA_VERSION)
                .status(OutboxStatusEnum.NEW.name())
                .retryCount(0)
                .maxRetries(properties.getRocketmq().getOutboxMaxRetries())
                .nextRetryTime(now)
                .createTime(now)
                .updateTime(now)
                .versionId(0)
                .build();
        if (diaryAiMapper.insertOutbox(outbox) != 1) {
            throw new IllegalStateException("AI终态失败事件写入Outbox失败, taskId=" + task.getId());
        }
    }

    private ConsumeResult handleOwnershipLost(Long taskId) {
        AiTaskPO currentTask = diaryAiMapper.selectAiTaskByTaskId(taskId);
        if (currentTask != null) {
            log.info("AI task is owned or completed elsewhere, taskId={}, status={}, workerId={}, version={}",
                    taskId, currentTask.getStatus(), currentTask.getWorkerId(), currentTask.getVersionId());
            return ConsumeResult.SUCCESS;
        }
        return ConsumeResult.FAILURE;
    }

    private String truncateErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            return "未提供异常信息";
        }
        return message.length() <= MAX_ERROR_MSG_LENGTH ? message : message.substring(0, MAX_ERROR_MSG_LENGTH);
    }
}
