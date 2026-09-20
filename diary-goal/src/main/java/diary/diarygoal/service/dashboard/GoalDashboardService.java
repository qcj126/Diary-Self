package diary.diarygoal.service.dashboard;

import diary.common.entity.goal.dto.GoalDashboardQueryDTO;
import diary.common.entity.goal.vo.GoalDashboardVO;
import diary.common.result.ApiResponse;

public interface GoalDashboardService {
    ApiResponse<GoalDashboardVO> getReview(GoalDashboardQueryDTO query, Long userId);
}
