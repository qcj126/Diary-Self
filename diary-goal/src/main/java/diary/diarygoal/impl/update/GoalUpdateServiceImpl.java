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

        StageGoalPO stageGoalPO = DTOConvertToPO.stageGoalDTOConvertToStageGoalPO(stageGoalDTO, existGoal.getId());
        goalMapper.updateStageGoalById(stageGoalPO);

        if (stageGoalDTO.getSubGoals() != null) {
            for (SubGoalDTO subGoalDTO : stageGoalDTO.getSubGoals()) {
                if (subGoalDTO == null || isBlank(subGoalDTO.getTitle())) {
                    continue;
                }
                if (subGoalDTO.getId() == null) {
                    goalMapper.insertSubGoal(buildNewSubGoalPO(existGoal, stageGoalDTO, subGoalDTO));
                } else {
                    goalMapper.updateSubGoalById(buildUpdateSubGoalPO(stageGoalDTO.getId(), subGoalDTO));
                }
            }
        }

        return ApiResponse.success("goal updated successfully");
    }

    private SubGoalPO buildNewSubGoalPO(StageGoalPO existGoal, StageGoalDTO stageGoalDTO, SubGoalDTO subGoalDTO) {
        SubGoalPO subGoalPO = DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoalDTO, MyUtils.getPrimaryKey());
        subGoalPO.setId(MyUtils.getPrimaryKey());
        subGoalPO.setUserId(stageGoalDTO.getUserId() == null ? existGoal.getUserId() : stageGoalDTO.getUserId());
        subGoalPO.setDeleted(false);
        return subGoalPO;
    }

    private SubGoalPO buildUpdateSubGoalPO(Long stageGoalId, SubGoalDTO subGoalDTO) {
        return DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoalDTO, MyUtils.getPrimaryKey());
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
