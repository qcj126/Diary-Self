package diary.diarygoal.mapper;

import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoalMapper {
    int insertStageGoal(StageGoalPO stageGoalPO);

    int insertSubGoal(SubGoalPO subGoalPO);

    StageGoalPO selectStageGoalById(@Param("id") Long id);

    List<SubGoalPO> selectSubGoalsByStageId(@Param("stageId") Long stageId);

    List<SubGoalPO> selectSubGoalsByStageIds(@Param("stageIds") List<Long> stageIds);

    List<StageGoalPO> selectStageGoals();

    int updateStageGoalById(StageGoalPO stageGoalPO);

    int updateSubGoalById(SubGoalPO subGoalPO);

    int deleteStageGoalById(@Param("id") Long id);

    int deleteSubGoalsByStageId(@Param("stageId") Long stageId);

    int deleteSubGoalById(@Param("id") Long id);

    List<StageGoalPO> queryGoalData(@Param("lastDays") Integer lastDays, @Param("exportSize") Integer exportSize);

    int batchInsertSubGoal(List<SubGoalPO> subGoalPOS);

}
