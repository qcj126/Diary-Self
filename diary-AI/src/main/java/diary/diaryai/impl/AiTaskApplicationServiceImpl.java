package diary.diaryai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.ai.vo.AiTaskSubmitVo;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.service.AiTaskApplicationService;
import diary.diaryai.service.AiTaskMessageProducer;
import diary.utils.commonutil.MyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.message.MessageId;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskApplicationServiceImpl implements AiTaskApplicationService {
    private static final Long FIRST_VERSION_USER_ID = 10000L;
    private static final String TASK_TYPE = "QWEN_PLUS_NUTRIENT";
    private static final int MAX_ATTEMPTS = 3;

    private final AiTaskMessageProducer rocketMqHandlerService;
    private final DiaryAiMapper diaryAiMapper;
    private final ObjectMapper objectMapper;
    @Override
    public AiTaskSubmitVo submitTask(AiInvokeDTO aiInvokeDTO) {
        validateAndNormalizeRequest(aiInvokeDTO);

        final Long userId = FIRST_VERSION_USER_ID;
        final String clientRequestId = aiInvokeDTO.getClientRequestId();

        // 已存在的任务直接返回，未创建的任务继续处理
        AiTaskPO existingTask = diaryAiMapper.selectByUserIdAndClientRequestId(userId, clientRequestId);
        if (existingTask != null) {
            return toSubmitVo(existingTask, "该请求已提交");
        }

        final String inputSnapshot;
        try {
            inputSnapshot = objectMapper.writeValueAsString(aiInvokeDTO);
        } catch (JsonProcessingException e) {
            // 序列化失败时直接抛出异常，此时未创建任务、未发送mq消息
            throw new IllegalArgumentException("AI任务输入快照序列化失败", e);
        }

        final Long taskId = MyUtils.getPrimaryKey();
        AiTaskPO aiTaskPO = AiTaskPO.builder()
                .id(taskId)
                .userId(userId)
                .clientRequestId(clientRequestId)
                .taskType(TASK_TYPE)
                .status("PENDING")
                .inputSnapshot(inputSnapshot)
                .attemptCount(0)
                .maxAttempts(MAX_ATTEMPTS)
                .workerId(null)
                .leaseUntil(null)
                .aiInfoId(null)
                .errorCode(null)
                .errorMessage(null)
                .createTime(LocalDateTime.now())
                .queueTime(null)
                .startTime(null)
                .finishTime(null)
                .versionId(0)
                .build();

        try {
            int inserted = diaryAiMapper.insertAiTask(aiTaskPO);
            if (inserted != 1) {
                throw new IllegalStateException("AI任务创建失败，数据库受影响行数: " + inserted);
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            // aiTask表有userId和clientRequestId联合唯一索引
            // 当并发请求同时插入时，后插入的请求会抛出 DuplicateKeyException
            AiTaskPO concurrentTask = diaryAiMapper.selectByUserIdAndClientRequestId(userId, clientRequestId);
            if (concurrentTask == null) {
                // 两个不同请求在极短时间到达，taskId生成相同，但是userId或clientRequestId不同，那么就查不到数据
                // 此时就不是联合唯一索引冲突，而是主键冲突了
                throw duplicateKeyException;
            }
            return toSubmitVo(concurrentTask, "该请求已提交");
        }

        AiTaskMessageDto aiTaskMessageDto = AiTaskMessageDto.builder()
                .eventId("evt-" + MyUtils.getPrimaryKey())
                .taskId(taskId)
                .userId(userId)
                .clientRequestId(clientRequestId)
                .taskType(TASK_TYPE)
                .schemaVersion(1)
                .occurTime(LocalDateTime.now())
                .traceId(MDC.get("traceId"))
                .build();

        final SendReceipt receipt;
        try {
            receipt = rocketMqHandlerService.send(aiTaskMessageDto);
        } catch (RuntimeException sendException) {
            /*
             * 以前发送失败时给 PENDING 写 finishTime 和业务错误，造成“尚待补发但看起来已结束”的矛盾状态。
             * 现在任务保持纯粹的 PENDING，finishTime 仍为 NULL，后续可由 PENDING 补偿任务或 Outbox 重发。
             */
            log.error("AI task message send failed, taskId={}, eventId={}",
                    taskId, aiTaskMessageDto.getEventId(), sendException);
            throw sendException;
        }

        MessageId messageId = receipt.getMessageId();
        log.info("AI task message sent, taskId={}, eventId={}, messageId={}",
                taskId, aiTaskMessageDto.getEventId(), messageId);

        AiTaskProcessDto queuedRequest = AiTaskProcessDto.builder()
                .taskId(taskId)
                .userId(userId)
                .clientRequestId(clientRequestId)
                .queueTime(LocalDateTime.now())
                .build();
        int updated = diaryAiMapper.markQueuedIfPending(queuedRequest);

        /*
         * Broker 确认后，Consumer 可能先于 Producer 的 QUEUED 更新收到消息并抢占 PENDING。
         * 以前无条件写 QUEUED 会把 RUNNING/SUCCESS 倒退；现在 SQL 只允许 PENDING -> QUEUED，
         * 更新为 0 时重新读取真实状态并返回。
         *
         * 此段故意放在 send 的 catch 外：以前“消息已发送但 QUEUED 更新失败”会被错误记录成发送失败，
         * 运维会误判 Broker 状态。现在发送异常和发送后的数据库异常具有不同日志语义。
         */
        AiTaskPO currentTask = diaryAiMapper.selectAiTaskByTaskId(taskId);
        if (currentTask == null) {
            throw new IllegalStateException("MQ发送成功后无法查询AI任务: " + taskId);
        }
        if (updated == 0) {
            log.info("AI task 在修改状态为 QUEUED 前，可能已被其他 Consumer 抢占并将状态更改为其余状态， taskId={}, currentStatus={}",
                    taskId, currentTask.getStatus());
        }
        return toSubmitVo(currentTask, "AI分析任务正在处理中");
    }

    private void validateAndNormalizeRequest(AiInvokeDTO request) {
        MyUtils.check()
                .notNull(request, "aiInvokeDTO")
                .notEmpty(request.getClientRequestId(), "clientRequestId")
                .notNull(request.getAiType(), "aiType")
                .notNull(request.getAiApplication(), "aiApplication")
                .notEmpty(request.getFlag(), "flag")
                .notNull(request.getMaterials(), "materials")
                .stringKeyMapNotContainsEmpty(request.getMaterials(), "materials")
                .notNull(request.getUniversalId(), "universalId");

        request.setClientRequestId(request.getClientRequestId().trim());
        request.setFlag(request.getFlag().trim().toUpperCase());
    }

    private AiTaskSubmitVo toSubmitVo(AiTaskPO task, String message) {
        return AiTaskSubmitVo.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .message(message)
                .build();
    }
}
