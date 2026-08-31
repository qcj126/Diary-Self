package diary.diaryai.mapper;

import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.mq.po.MqOutboxPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiaryAiMapper {
    int insertAiNutrient(AiNutrientPO aiNutrientPO);

    int insertAiInfo(AiInfoPO aiInfoPO);

    int insertAiTask(AiTaskPO aiTaskPO);

    AiTaskPO selectAiTaskByTaskId(@Param("taskId") Long taskId);

    AiTaskPO selectAiTaskByTaskIdAndUserId(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );

    AiTaskPO selectByUserIdAndClientRequestId(
            @Param("userId") Long userId,
            @Param("clientRequestId") String clientRequestId
    );

    /*
     * 以前所有状态都通过一个无前置状态限制的通用 UPDATE 修改，旧 Worker、Producer 和重复消息
     * 可以互相覆盖状态，甚至把 SUCCESS 覆盖回 QUEUED。现在为每条状态迁移保留独立 Mapper，
     * 让 SQL 自己校验前置状态、Worker 所有权和乐观锁版本，调用方再根据受影响行数判断是否成功。
     */
    int claimForExecution(AiTaskProcessDto aiTaskProcessDto);

    int markRetryWaitIfOwned(AiTaskProcessDto aiTaskProcessDto);

    int markSuccessIfOwned(AiTaskProcessDto aiTaskProcessDto);

    int markFailedIfOwned(AiTaskProcessDto aiTaskProcessDto);

    int markFailedIfAttemptsExhausted(AiTaskProcessDto aiTaskProcessDto);

    int recoverExpiredRunning(AiTaskProcessDto aiTaskProcessDto);

    int markDeadLetterIfDispatchable(AiTaskProcessDto aiTaskProcessDto);

    int renewExecutionLease(AiTaskProcessDto aiTaskProcessDto);

    int recoverStaleWaiting(
            @Param("taskId") Long taskId,
            @Param("versionId") Integer versionId,
            @Param("waitingRecoverySeconds") long waitingRecoverySeconds,
            @Param("maxRecoveryMessages") int maxRecoveryMessages,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    int updateRequestHashIfNull(
            @Param("taskId") Long taskId,
            @Param("requestHash") String requestHash
    );

    int insertOutbox(MqOutboxPO outbox);

    List<MqOutboxPO> selectReadyOutbox(@Param("limit") int limit);

    MqOutboxPO selectOutboxById(@Param("id") Long id);

    List<MqOutboxPO> selectTimedOutbox(
            @Param("timeoutSeconds") long timeoutSeconds,
            @Param("limit") int limit
    );

    int claimOutbox(
            @Param("id") Long id,
            @Param("versionId") Integer versionId
    );

    int markOutboxSent(
            @Param("id") Long id,
            @Param("versionId") Integer versionId,
            @Param("brokerMessageId") String brokerMessageId
    );

    int markOutboxRetry(
            @Param("id") Long id,
            @Param("versionId") Integer versionId,
            @Param("retryDelaySeconds") long retryDelaySeconds,
            @Param("lastError") String lastError
    );

    int markOutboxDead(
            @Param("id") Long id,
            @Param("versionId") Integer versionId,
            @Param("lastError") String lastError
    );

    int markQueuedByTaskIdIfWaiting(@Param("taskId") Long taskId);

    List<AiTaskPO> selectExpiredRunningTasks(@Param("limit") int limit);

    List<AiTaskPO> selectStaleWaitingTasks(
            @Param("waitingRecoverySeconds") long waitingRecoverySeconds,
            @Param("maxRecoveryMessages") int maxRecoveryMessages,
            @Param("limit") int limit
    );

    int countActiveTaskDispatchOutbox(@Param("taskId") Long taskId);

    AiNutrientPO selectAiNutrientByTaskId(@Param("taskId") Long taskId);

    int insertRetryTaskOutbox(MqOutboxPO outbox);

    int deleteExpiredSentOutbox(
            @Param("retentionDays") int retentionDays,
            @Param("limit") int limit
    );
}
