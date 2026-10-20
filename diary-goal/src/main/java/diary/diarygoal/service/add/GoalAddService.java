package diary.diarygoal.service.add;

import diary.diarygoal.entity.dto.StageGoalDTO;
import diary.diarygoal.entity.dto.SubGoalDTO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface GoalAddService {
    ApiResponse<String> addGoal(StageGoalDTO stageGoalDTO);

    ApiResponse<String> batchAddSubGoal(List<SubGoalDTO> subGoalDTOList);
}
