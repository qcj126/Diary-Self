package diary.diaryai.impl;

import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.mq.po.MqOutboxPO;
import diary.common.enums.aienum.AiTaskErrorCodeEnum;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.common.enums.outbox.OutboxEventTypeEnum;
import diary.common.enums.outbox.OutboxStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.recovery.event.TaskRecoveredEvent;
import diary.diaryai.service.AiTaskCommandService;
import diary.diaryai.service.AiTaskRecoveryService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static diary.common.consts.AiTaskConst.AI_TASK_AGGREGATE_TYPE;
import static diary.common.consts.AiTaskConst.OUTBOX_EVENT_ID;
import static diary.common.consts.AiTaskConst.OUTBOX_SCHEMA_VERSION;
import static diary.utils.commonutil.MyUtils.writeJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskRecoveryServiceImpl implements AiTaskRecoveryService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final AiTaskCommandService aiTaskCommandService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recover(AiTaskPO task) {
        LocalDateTime now = LocalDateTime.now();

        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            /*
             * 改前：Recovery 自己 UPDATE FAILED、再自行拼失败 Outbox；其中 Outbox 插入返回 0 时只告警不回滚。
             * 改后：与 Consumer 共用 failExhaustedTask，FAILED 与 AI_FAILED Outbox 是同一个不可拆分事务动作。
             * 效果：消除两个失败实现逐渐漂移，也消除“FAILED 已提交但失败事件没提交”的半状态。
             */
            if (aiTaskCommandService.failExhaustedTask(
                    task, "RUNNING租约过期且尝试次数已耗尽")) {
                publishCacheEvictAfterCommit(task.getId());
            }
            return;
        }

        AiTaskProcessDto retry = AiTaskProcessDto.builder()
                .taskId(task.getId())
                .versionId(task.getVersionId())
                .errorCode(AiTaskErrorCodeEnum.RETRYABLE_ERROR.name())
                .errorMessage("RUNNING租约过期，等待恢复")
                .build();

        if (diaryAiMapper.recoverExpiredRunning(retry) != 1) {
            // 多实例 Recovery 同时扫描很常见；CAS 失败表示其他实例已处理，不应当作为任务故障抛异常。
            return;
        }
        insertRetryTaskOutbox(task, now);
        publishCacheEvictAfterCommit(task.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverWaiting(AiTaskPO task) {
        if (diaryAiMapper.countActiveTaskDispatchOutbox(task.getId()) > 0) {
            // Outbox Publisher 仍有可执行记录，交给正常投递链路，避免 Recovery 制造无意义重复消息。
            return;
        }

        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            if (aiTaskCommandService.failExhaustedTask(
                    task, "等待态任务的执行次数已耗尽")) {
                publishCacheEvictAfterCommit(task.getId());
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int currentRecoveryCount = task.getRecoveryCount() == null ? 0 : task.getRecoveryCount();
        boolean reachesRecoveryLimit = currentRecoveryCount + 1
                >= properties.getTask().getWaitingMaxRecoveryMessages();
        String errorCode = reachesRecoveryLimit
                ? AiTaskErrorCodeEnum.DISPATCH_RECOVERY_EXHAUSTED.name()
                : AiTaskErrorCodeEnum.RETRYABLE_ERROR.name();
        String errorMessage = reachesRecoveryLimit
                ? "等待态消息已达自动补发上限，保留任务等待迟到消息或人工处理"
                : "等待态消息可能延迟或进入DLQ，创建恢复消息";
        int recovered = diaryAiMapper.recoverStaleWaiting(
                task.getId(),
                task.getVersionId(),
                properties.getTask().getWaitingRecoverySeconds(),
                properties.getTask().getWaitingMaxRecoveryMessages(),
                errorCode,
                errorMessage
        );
        if (recovered != 1) {
            return;
        }

        /*
         * 改前：Recovery Job 只扫描 RUNNING；本地并发满、数据库暂时异常或 MQ 重试进入 DLQ 后，
         * task 会永久停在 PENDING/QUEUED/RETRY_WAIT。
         * 改后：等待超过阈值且没有活跃投递 Outbox 时，用 version CAS 更新状态，并在同一事务创建恢复 Outbox。
         * 效果：非 RUNNING 的消息丢失可以有界补发；多实例同时扫描只会有一个实例恢复成功，
         * 同时不会把 Broker 积压误判成业务终态。
         */
        insertRetryTaskOutbox(task, now);
        if (reachesRecoveryLimit) {
            /*
             * SENT 只表示 Broker 已接收，不能证明 Consumer 已消费。补发耗尽时不能把
             * 可能仅是积压的任务改成终态；保留等待态，迟到消息仍可正常抢占。
             */
            log.error("AI任务自动补发已达上限，需要检查Consumer Lag/DLQ, taskId={}, recoveryCount={}",
                    task.getId(), currentRecoveryCount + 1);
        }
        publishCacheEvictAfterCommit(task.getId());
    }

    private void insertRetryTaskOutbox(AiTaskPO task, LocalDateTime now) {
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
                .tag(properties.getRocketmq().getTaskTag())
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

    private void publishCacheEvictAfterCommit(Long taskId) {
        eventPublisher.publishEvent(new TaskRecoveredEvent(this, taskId));
    }
}
