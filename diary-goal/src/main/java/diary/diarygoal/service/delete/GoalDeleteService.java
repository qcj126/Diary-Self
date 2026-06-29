package diary.diarygoal.service.delete;

import diary.common.result.ApiResponse;

public interface GoalDeleteService {
    ApiResponse<String> deleteGoal(Long id);
}
