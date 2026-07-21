package diary.diarygoal.impl.add;

import diary.common.convert.goal.DTOConvertToPO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.add.GoalAddService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalAddServiceImpl implements GoalAddService {
    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addGoal(StageGoalDTO stageGoalDTO) {
        validateStageGoal(stageGoalDTO);

        Long stageGoalId = MyUtils.getPrimaryKey();
        stageGoalDTO.setId(stageGoalId);
        StageGoalPO stageGoalPO = DTOConvertToPO.stageGoalDTOConvertToStageGoalPO(stageGoalDTO);
        goalMapper.insertStageGoal(stageGoalPO);

        if (stageGoalDTO.getSubGoals() != null) {
            for (SubGoalDTO subGoalDTO : stageGoalDTO.getSubGoals()) {
                if (subGoalDTO == null || isBlank(subGoalDTO.getTitle())) {
                    continue;
                }
                subGoalDTO.setId(MyUtils.getPrimaryKey());
                subGoalDTO.setStageGoalId(stageGoalId);
                subGoalDTO.setUserId(stageGoalDTO.getUserId());
                SubGoalPO subGoalPO = DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoalDTO);
                goalMapper.insertSubGoal(subGoalPO);
            }
        }

        return ApiResponse.success("新增目标成功");
    }

    private void validateStageGoal(StageGoalDTO stageGoalDTO) {
        if (stageGoalDTO == null) {
            throw new ParamIllegalException("request body is empty");
        }
        if (stageGoalDTO.getUserId() == null || isBlank(stageGoalDTO.getCategory())
                || isBlank(stageGoalDTO.getTitle()) || isBlank(stageGoalDTO.getDescription())) {
            throw new ParamIllegalException("required parameters cannot be empty");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
