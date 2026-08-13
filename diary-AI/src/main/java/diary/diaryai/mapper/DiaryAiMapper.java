package diary.diaryai.mapper;

import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DiaryAiMapper {
    int insertAiNutrient(AiNutrientPO aiNutrientPO);

    int insertAiInfo(AiInfoPO aiInfoPO);

    int insertAiTask(AiTaskPO aiTaskPO);

    AiTaskPO selectAiTaskByTaskId(@Param("taskId") Long taskId);

    AiTaskPO selectByUserIdAndClientRequestId(
            @Param("userId") Long userId,
            @Param("clientRequestId") String clientRequestId
    );

    /*
     * 以前所有状态都通过一个无前置状态限制的通用 UPDATE 修改，旧 Worker、Producer 和重复消息
     * 可以互相覆盖状态，甚至把 SUCCESS 覆盖回 QUEUED。现在为每条状态迁移保留独立 Mapper，
     * 让 SQL 自己校验前置状态、Worker 所有权和乐观锁版本，调用方再根据受影响行数判断是否成功。
     */
    int markQueuedIfPending(AiTaskProcessDto aiTaskProcessDto);

    int claimForExecution(AiTaskProcessDto aiTaskProcessDto);

    int markRetryWaitIfOwned(AiTaskProcessDto aiTaskProcessDto);

    int markSuccessIfOwned(AiTaskProcessDto aiTaskProcessDto);

    int markFailedIfOwned(AiTaskProcessDto aiTaskProcessDto);

    int markFailedIfAttemptsExhausted(AiTaskProcessDto aiTaskProcessDto);

    int recoverExpiredRunning(AiTaskProcessDto aiTaskProcessDto);
}
