package diary.diarygoal.impl.delete;

import diary.common.entity.goal.po.StageGoalPO;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.delete.GoalDeleteService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
public class GoalDeleteServiceImpl implements GoalDeleteService {
    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteGoal(Long id) {
        try {
            MyUtils.check().notNull(id, "id");
            StageGoalPO stageGoalPO = goalMapper.selectStageGoalById(id);
            if (stageGoalPO == null) {
                return ApiResponse.delFail();
            }
            goalMapper.deleteSubGoalsByStageId(id);
            if (goalMapper.deleteStageGoalById(id) <= 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return ApiResponse.delFail();
            }
            return ApiResponse.success("deleted successfully");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.delFail();
        }
    }
}
