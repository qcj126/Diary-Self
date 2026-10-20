package diary.diarygoal.service.update;

import diary.diarygoal.entity.dto.StageGoalDTO;
import diary.common.result.ApiResponse;

public interface GoalUpdateService {
    ApiResponse<String> updateGoal(StageGoalDTO stageGoalDTO);
}
