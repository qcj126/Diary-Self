package diary.diarygoal.impl.update;

import diary.common.convert.goal.DTOConvertToPO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.update.GoalUpdateService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoalUpdateServiceImpl implements GoalUpdateService {
    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateGoal(StageGoalDTO stageGoalDTO) {
        if (stageGoalDTO == null) {
            return ApiResponse.fail(400, "request body is empty");
        }
        if (stageGoalDTO.getId() == null) {
            throw new ParamIllegalException("goal id cannot be empty");
        }
        StageGoalPO existGoal = goalMapper.selectStageGoalById(stageGoalDTO.getId());
        if (existGoal == null) {
            throw new ParamIllegalException("goal does not exist");
        }

        StageGoalPO stageGoalPO = DTOConvertToPO.stageGoalDTOConvertToStageGoalPO(stageGoalDTO);
        goalMapper.updateStageGoalById(stageGoalPO);

        if (stageGoalDTO.getSubGoals() != null) {
            List<SubGoalDTO> subGoalsDTO = stageGoalDTO.getSubGoals();
            if (subGoalsDTO.stream().anyMatch(subGoalDTO ->
                    (subGoalDTO.getTitle() == null || subGoalDTO.getTitle().trim().isEmpty())
                            || subGoalDTO.getContent() == null || subGoalDTO.getContent().trim().isEmpty()
                            || subGoalDTO.getEstimatedHours() == null || subGoalDTO.getEstimatedHours().compareTo(BigDecimal.ZERO) <= 0
            )) {
                throw new ParamIllegalException("小目标标题、内容和估计小时数不能为空");
            }
            List<SubGoalPO> subGoalPOS = new ArrayList<>();
            subGoalsDTO.forEach(subGoal -> subGoalPOS.add(DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoal)));
            goalMapper.batchUpdateSubGoals(subGoalPOS);
        }

        return ApiResponse.success("goal updated successfully");
    }
}
