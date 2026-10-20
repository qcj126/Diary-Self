package diary.diarygoal.service.query;

import diary.diarygoal.entity.dto.GoalQueryDTO;
import diary.diarygoal.entity.vo.StageGoalVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface GoalQueryService {
    ApiResponse<StageGoalVO> getGoalById(Long id);

    ApiResponse<List<StageGoalVO>> queryGoals(GoalQueryDTO goalQueryDTO);
}
