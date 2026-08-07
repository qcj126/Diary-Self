package diary.diarygoal.impl.update;

import diary.common.convert.goal.DTOConvertToPO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.update.GoalUpdateService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoalUpdateServiceImpl implements GoalUpdateService {
    private static final Long DEFAULT_USER_ID = 10000L;
    private static final long DEFAULT_DDL_DAYS = 7L;

    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateGoal(StageGoalDTO stageGoalDTO) {
        try {
            MyUtils.check().notNull(stageGoalDTO, "stageGoal").notNull(stageGoalDTO.getId(), "id");

            StageGoalPO existGoal = goalMapper.selectStageGoalById(stageGoalDTO.getId());
            if (existGoal == null) {
                return ApiResponse.updateFail();
            }

            StageGoalPO stageGoalPO = DTOConvertToPO.stageGoalDTOConvertToStageGoalPO(stageGoalDTO);
            stageGoalPO.setUserId(stageGoalPO.getUserId() == null ? existGoal.getUserId() : stageGoalPO.getUserId());
            if (goalMapper.updateStageGoalById(stageGoalPO) <= 0) {
                return ApiResponse.updateFail();
            }

            ApiResponse<String> subGoalResult = saveSubGoals(stageGoalDTO.getSubGoals(), stageGoalDTO.getId(), stageGoalPO.getUserId());
            if (subGoalResult.getCode() != 200) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return subGoalResult;
            }

            return ApiResponse.success("goal updated successfully");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.updateFail();
        }
    }

    private ApiResponse<String> saveSubGoals(List<SubGoalDTO> subGoalsDTO, Long stageGoalId, Long userId) {
        if (subGoalsDTO == null || subGoalsDTO.isEmpty()) {
            return ApiResponse.success("no sub goals");
        }

        List<SubGoalPO> newSubGoals = new ArrayList<>();
        for (SubGoalDTO subGoalDTO : subGoalsDTO) {
            validateSubGoal(subGoalDTO);
            SubGoalPO subGoalPO = DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoalDTO);
            subGoalPO.setStageId(stageGoalId);
            subGoalPO.setUserId(resolveUserId(subGoalPO.getUserId(), userId));
            subGoalPO.setEndTime(defaultEndTime(subGoalPO.getEndTime()));

            if (subGoalPO.getId() == null) {
                subGoalPO.setId(MyUtils.getPrimaryKey());
                newSubGoals.add(subGoalPO);
            } else if (goalMapper.updateSubGoalById(subGoalPO) <= 0) {
                return ApiResponse.updateFail();
            }
        }

        if (!newSubGoals.isEmpty() && goalMapper.batchInsertSubGoal(newSubGoals) != newSubGoals.size()) {
            return ApiResponse.updateFail();
        }
        return ApiResponse.success("sub goals saved");
    }

    private void validateSubGoal(SubGoalDTO subGoalDTO) {
        MyUtils.check()
                .notNull(subGoalDTO, "subGoal")
                .notEmpty(subGoalDTO.getTitle(), "title")
                .notEmpty(subGoalDTO.getContent(), "content")
                .notNull(subGoalDTO.getEstimatedHours(), "estimatedHours");
        if (subGoalDTO.getEstimatedHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("estimatedHours must be greater than zero");
        }
    }

    private Long resolveUserId(Long userId, Long fallbackUserId) {
        if (userId != null) {
            return userId;
        }
        return fallbackUserId == null ? DEFAULT_USER_ID : fallbackUserId;
    }

    private LocalDateTime defaultEndTime(LocalDateTime endTime) {
        return endTime == null ? LocalDateTime.now().plusDays(DEFAULT_DDL_DAYS) : endTime;
    }
}
