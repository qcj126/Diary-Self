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

        fillStageGoalDefaults(stageGoalDTO, existGoal);
        StageGoalPO stageGoalPO = DTOConvertToPO.stageGoalDTOConvertToStageGoalPO(stageGoalDTO);
        goalMapper.updateStageGoalById(stageGoalPO);

        if (stageGoalDTO.getSubGoals() != null) {
            for (SubGoalDTO subGoalDTO : stageGoalDTO.getSubGoals()) {
                if (subGoalDTO == null || isBlank(subGoalDTO.getTitle())) {
                    continue;
                }
                subGoalDTO.setStageGoalId(existGoal.getId());
                subGoalDTO.setUserId(stageGoalDTO.getUserId());
                if (subGoalDTO.getId() == null) {
                    subGoalDTO.setId(MyUtils.getPrimaryKey());
                    goalMapper.insertSubGoal(DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoalDTO));
                } else {
                    goalMapper.updateSubGoalById(buildUpdateSubGoalPO(subGoalDTO));
                }
            }
        }

        return ApiResponse.success("goal updated successfully");
    }

    private void fillStageGoalDefaults(StageGoalDTO stageGoalDTO, StageGoalPO existGoal) {
        stageGoalDTO.setId(existGoal.getId());
        if (stageGoalDTO.getUserId() == null) {
            stageGoalDTO.setUserId(existGoal.getUserId());
        }
        if (isBlank(stageGoalDTO.getCreator())) {
            stageGoalDTO.setCreator(existGoal.getCreator());
        }
        if (isBlank(stageGoalDTO.getCategory())) {
            stageGoalDTO.setCategory(existGoal.getCategory());
        }
        if (isBlank(stageGoalDTO.getTitle())) {
            stageGoalDTO.setTitle(existGoal.getTitle());
        }
        if (stageGoalDTO.getDescription() == null) {
            stageGoalDTO.setDescription(existGoal.getDescription());
        }
    }

    private SubGoalPO buildUpdateSubGoalPO(SubGoalDTO subGoalDTO) {
        return SubGoalPO.builder()
                .id(subGoalDTO.getId())
                .stageGoalId(subGoalDTO.getStageGoalId())
                .userId(subGoalDTO.getUserId())
                .title(subGoalDTO.getTitle())
                .content(subGoalDTO.getContent())
                .learnedHours(subGoalDTO.getLearnedHours())
                .estimatedHours(subGoalDTO.getEstimatedHours())
                .deleted(false)
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
