package diary.diarygoal.mapper;

import diary.common.entity.goal.dto.GoalCheckinQueryDTO;
import diary.common.entity.goal.po.GoalCheckinPO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface GoalCheckinMapper {
    int insertCheckin(GoalCheckinPO checkin);

    GoalCheckinPO selectCheckinById(@Param("id") Long id, @Param("userId") Long userId);

    GoalCheckinPO selectCheckinByRequestId(@Param("requestId") String requestId,
                                          @Param("userId") Long userId);

    List<GoalCheckinPO> selectCheckins(@Param("userId") Long userId,
                                      @Param("query") GoalCheckinQueryDTO query,
                                      @Param("offset") Integer offset,
                                      @Param("pageSize") Integer pageSize);

    int updateCheckin(GoalCheckinPO checkin);

    int softDeleteCheckin(@Param("id") Long id, @Param("userId") Long userId);

    int softDeleteCheckinsByStageGoalId(@Param("stageGoalId") Long stageGoalId,
                                        @Param("userId") Long userId);

    StageGoalPO selectStageGoalForUpdate(@Param("stageGoalId") Long stageGoalId,
                                         @Param("userId") Long userId);

    SubGoalPO selectSubGoalForUpdate(@Param("subGoalId") Long subGoalId,
                                     @Param("stageGoalId") Long stageGoalId,
                                     @Param("userId") Long userId);

    int adjustSubGoalLearnedHours(@Param("subGoalId") Long subGoalId,
                                  @Param("stageGoalId") Long stageGoalId,
                                  @Param("userId") Long userId,
                                  @Param("hoursDelta") BigDecimal hoursDelta);

    int updateSubGoalStatus(@Param("subGoalId") Long subGoalId,
                            @Param("stageGoalId") Long stageGoalId,
                            @Param("userId") Long userId,
                            @Param("status") Integer status,
                            @Param("completedTime") LocalDateTime completedTime);

    int updateStageGoalStatus(@Param("stageGoalId") Long stageGoalId,
                              @Param("userId") Long userId,
                              @Param("status") Integer status,
                              @Param("completedTime") LocalDateTime completedTime);

    Integer selectLatestSubGoalActionType(@Param("subGoalId") Long subGoalId,
                                          @Param("stageGoalId") Long stageGoalId,
                                          @Param("userId") Long userId);

    Integer selectLatestStageGoalActionType(@Param("stageGoalId") Long stageGoalId,
                                            @Param("userId") Long userId);

    int countSubGoalCheckins(@Param("subGoalId") Long subGoalId,
                             @Param("stageGoalId") Long stageGoalId,
                             @Param("userId") Long userId);

    int countStageGoalCheckins(@Param("stageGoalId") Long stageGoalId,
                               @Param("userId") Long userId);

    int countSubGoals(@Param("stageGoalId") Long stageGoalId,
                      @Param("userId") Long userId);

    int countIncompleteSubGoals(@Param("stageGoalId") Long stageGoalId,
                                @Param("userId") Long userId);
}
