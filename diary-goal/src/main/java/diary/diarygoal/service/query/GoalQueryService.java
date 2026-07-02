package diary.diarygoal.service.query;

import diary.common.entity.goal.dto.GoalQueryDTO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.vo.StageGoalVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface GoalQueryService {
    ApiResponse<StageGoalVO> getGoalById(Long id);

    ApiResponse<List<StageGoalVO>> queryGoals(GoalQueryDTO goalQueryDTO);
}
