package diary.diarygoal.service.dashboard;

import diary.diarygoal.entity.dto.GoalDashboardQueryDTO;
import diary.diarygoal.entity.vo.GoalDashboardVO;
import diary.common.result.ApiResponse;

public interface GoalDashboardService {
    ApiResponse<GoalDashboardVO> getReview(GoalDashboardQueryDTO query, Long userId);
}
