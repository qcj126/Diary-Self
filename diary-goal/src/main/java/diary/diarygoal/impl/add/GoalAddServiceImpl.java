package diary.diarygoal.impl.add;

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

import java.math.BigDecimal;

@Service
public class GoalAddServiceImpl implements GoalAddService {
    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addGoal(StageGoalDTO stageGoalDTO) {
        if (stageGoalDTO == null) {
            return ApiResponse.fail(400, "request body is empty");
        }
        if (stageGoalDTO.getUserId() == null
                || isBlank(stageGoalDTO.getCategory())
                || isBlank(stageGoalDTO.getTitle())
                || isBlank(stageGoalDTO.getDescription())) {
            throw new ParamIllegalException("required parameters cannot be empty");
        }

        Long stageGoalId = MyUtils.getPrimaryKey();
        StageGoalPO stageGoalPO = new StageGoalPO();
        stageGoalPO.setId(stageGoalId);
        stageGoalPO.setUserId(stageGoalDTO.getUserId());
        stageGoalPO.setCreator(isBlank(stageGoalDTO.getCreator()) ? String.valueOf(stageGoalDTO.getUserId()) : stageGoalDTO.getCreator());
        stageGoalPO.setCategory(stageGoalDTO.getCategory());
        stageGoalPO.setTitle(stageGoalDTO.getTitle());
        stageGoalPO.setDescription(stageGoalDTO.getDescription());
        stageGoalPO.setDeleted(false);
        goalMapper.insertStageGoal(stageGoalPO);

        if (stageGoalDTO.getSubGoals() != null) {
            for (SubGoalDTO subGoalDTO : stageGoalDTO.getSubGoals()) {
                if (subGoalDTO == null || isBlank(subGoalDTO.getTitle())) {
                    continue;
                }
                goalMapper.insertSubGoal(buildSubGoalPO(stageGoalDTO.getUserId(), stageGoalId, subGoalDTO));
            }
        }

        return ApiResponse.success("goal added successfully");
    }

    private SubGoalPO buildSubGoalPO(Long userId, Long stageGoalId, SubGoalDTO subGoalDTO) {
        SubGoalPO subGoalPO = new SubGoalPO();
        subGoalPO.setId(MyUtils.getPrimaryKey());
        subGoalPO.setStageGoalId(stageGoalId);
        subGoalPO.setUserId(userId);
        subGoalPO.setTitle(subGoalDTO.getTitle());
        subGoalPO.setContent(subGoalDTO.getContent());
        subGoalPO.setLearnedHours(defaultZero(subGoalDTO.getLearnedHours()));
        subGoalPO.setEstimatedHours(defaultZero(subGoalDTO.getEstimatedHours()));
        subGoalPO.setDeleted(false);
        return subGoalPO;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
