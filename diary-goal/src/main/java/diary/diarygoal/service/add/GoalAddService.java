package diary.diarygoal.service.add;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface GoalAddService {
    ApiResponse<String> addGoal(StageGoalDTO stageGoalDTO);

    ApiResponse<String> batchAddSubGoal(List<SubGoalDTO> subGoalDTOList);
}
