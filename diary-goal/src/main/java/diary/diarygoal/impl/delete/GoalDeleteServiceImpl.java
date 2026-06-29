package diary.diarygoal.impl.delete;

import diary.common.entity.goal.po.StageGoalPO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.delete.GoalDeleteService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalDeleteServiceImpl implements GoalDeleteService {
    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteGoal(Long id) {
        if (id == null) {
            throw new ParamIllegalException("goal id cannot be empty");
        }
        StageGoalPO stageGoalPO = goalMapper.selectStageGoalById(id);
        if (stageGoalPO == null) {
            throw new ParamIllegalException("goal does not exist");
        }
        goalMapper.deleteStageGoalById(id);
        goalMapper.deleteSubGoalsByStageGoalId(id);
        return ApiResponse.success("deleted successfully");
    }
}
