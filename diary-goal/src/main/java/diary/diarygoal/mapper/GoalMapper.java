package diary.diarygoal.mapper;

import diary.common.entity.goal.dto.GoalQueryDTO;
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

    List<SubGoalPO> selectSubGoalsByStageGoalId(@Param("stageGoalId") Long stageGoalId);

    List<StageGoalPO> selectStageGoals(GoalQueryDTO goalQueryDTO);

    int updateStageGoalById(StageGoalPO stageGoalPO);

    int updateSubGoalById(SubGoalPO subGoalPO);

    int deleteStageGoalById(@Param("id") Long id);

    int deleteSubGoalsByStageGoalId(@Param("stageGoalId") Long stageGoalId);

    int deleteSubGoalById(@Param("id") Long id);
}
