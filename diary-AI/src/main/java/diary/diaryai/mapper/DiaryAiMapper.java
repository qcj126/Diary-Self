package diary.diaryai.mapper;

import diary.diaryai.entity.ai.dto.AiTaskProcessDto;
import diary.diaryai.entity.ai.po.AiInfoPO;
import diary.diaryai.entity.ai.po.AiNutrientPO;
import diary.diaryai.entity.ai.po.AiSentimentPO;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.entity.ai.vo.AiTaskResultVo;
import diary.diaryai.entity.ai.vo.AiSentimentTaskResultVo;
import diary.diaryai.entity.ai.vo.AiTaskSummaryVo;
import diary.diaryai.entity.mq.po.MqOutboxPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiaryAiMapper {
    int insertAiNutrient(AiNutrientPO aiNutrientPO);

    int insertAiSentiment(AiSentimentPO aiSentimentPO);

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

    int updateRequestHashIfNull(
            @Param("taskId") Long taskId,
            @Param("requestHash") String requestHash
    );

    int insertOutbox(MqOutboxPO outbox);

    List<MqOutboxPO> selectReadyOutbox(@Param("limit") int limit);

    // 当前时间-更新时间 = 过渡时间    过渡时间 - 超时时间 > 0 超时
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

    int insertRetryTaskOutbox(MqOutboxPO outbox);

    int deleteExpiredSentOutbox(
            @Param("retentionDays") int retentionDays,
            @Param("limit") int limit
    );

    List<AiTaskSummaryVo> selectAiTaskSummaryList(@Param("userId") Long userId,
                                                   @Param("taskStatus") String taskStatus,
                                                   @Param("applicationCode") Integer applicationCode,
                                                   @Param("title") String title,
                                                   @Param("pageSize") Integer pageSize,
                                                   @Param("offset") Integer offset);

    AiTaskResultVo selectNutrientTaskResult(@Param("taskId") Long taskId, @Param("userId") Long userId);

    AiSentimentTaskResultVo selectLoveRecordSentimentTaskResult(
            @Param("taskId") Long taskId,
            @Param("userId") Long userId
    );
}
