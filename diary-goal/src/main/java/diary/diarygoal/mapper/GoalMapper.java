package diary.diarygoal.mapper;

import diary.diarygoal.entity.po.StageGoalPO;
import diary.diarygoal.entity.po.SubGoalPO;
import diary.diarygoal.entity.dto.GoalQueryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoalMapper {
    int insertStageGoal(StageGoalPO stageGoalPO);

    StageGoalPO selectStageGoalById(@Param("id") Long id);

    SubGoalPO selectSubGoalById(@Param("id") Long id);

    List<SubGoalPO> selectSubGoalsByStageId(@Param("stageId") Long stageId);

    List<SubGoalPO> selectSubGoalsByStageIds(@Param("stageIds") List<Long> stageIds);

    List<StageGoalPO> selectStageGoals(GoalQueryDTO goalQueryDTO);

    int updateStageGoalById(StageGoalPO stageGoalPO);

    int updateSubGoalById(SubGoalPO subGoalPO);

    int deleteStageGoalById(@Param("id") Long id);

    int deleteSubGoalsByStageId(@Param("stageId") Long stageId);

    int batchInsertSubGoal(@Param("subGoalPOS") List<SubGoalPO> subGoalPOS);

    int batchUpdateSubGoals(@Param("subGoalPOS") List<SubGoalPO> subGoalPOS);
}
