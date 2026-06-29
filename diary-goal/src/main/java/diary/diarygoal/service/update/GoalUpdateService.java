package diary.diarygoal.service.update;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.result.ApiResponse;

public interface GoalUpdateService {
    ApiResponse<String> updateGoal(StageGoalDTO stageGoalDTO);
}
