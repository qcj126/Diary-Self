package diary.diarygoal.service.add;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.result.ApiResponse;

public interface GoalAddService {
    ApiResponse<String> addGoal(StageGoalDTO stageGoalDTO);
}
