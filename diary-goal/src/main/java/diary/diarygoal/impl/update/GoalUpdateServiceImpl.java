package diary.diarygoal.impl.update;

import diary.diarygoal.entity.convert.DTOConvertToPO;
import diary.diarygoal.entity.dto.StageGoalDTO;
import diary.diarygoal.entity.dto.SubGoalDTO;
import diary.diarygoal.entity.po.StageGoalPO;
import diary.diarygoal.entity.po.SubGoalPO;
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
    private static final long DEFAULT_DDL_DAYS = 7L;
    private static final int STAGE_GOAL_FLAG = 1;
    private static final int SUB_GOAL_FLAG = 2;
    private static final int COMPLETED_STATUS = 3;

    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateGoal(StageGoalDTO stageGoalDTO) {
        try {
            MyUtils.check().notNull(stageGoalDTO, "stageGoal").notNull(stageGoalDTO.getId(), "id");

            int flag = stageGoalDTO.getFlag() == null ? STAGE_GOAL_FLAG : stageGoalDTO.getFlag();
            if (flag == SUB_GOAL_FLAG) {
                return completeSubGoal(stageGoalDTO);
            }
            if (flag != STAGE_GOAL_FLAG) {
                return ApiResponse.fail(400, "flag 只能是 1 或 2");
            }
            if (stageGoalDTO.getStatus() != null) {
                return completeStageGoal(stageGoalDTO);
            }

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

    private ApiResponse<String> completeStageGoal(StageGoalDTO stageGoalDTO) {
        if (stageGoalDTO.getStatus() != COMPLETED_STATUS) {
            return ApiResponse.fail(400, "完成操作的 status 必须为 3");
        }

        StageGoalPO existGoal = goalMapper.selectStageGoalById(stageGoalDTO.getId());
        if (existGoal == null) {
            return ApiResponse.updateFail();
        }
        if (existGoal.getStatus() != null && existGoal.getStatus() == COMPLETED_STATUS) {
            return ApiResponse.success("stage goal already completed");
        }

        List<SubGoalPO> subGoals = goalMapper.selectSubGoalsByStageId(stageGoalDTO.getId());
        if (!subGoals.isEmpty() && subGoals.stream().anyMatch(item -> item.getStatus() == null || item.getStatus() != COMPLETED_STATUS)) {
            return ApiResponse.fail(400, "请先完成该阶段目标下的全部小目标");
        }

        StageGoalPO completedGoal = StageGoalPO.builder()
                .id(stageGoalDTO.getId())
                .status(COMPLETED_STATUS)
                .completedTime(LocalDateTime.now())
                .build();
        if (goalMapper.updateStageGoalById(completedGoal) <= 0) {
            return ApiResponse.updateFail();
        }
        return ApiResponse.success("stage goal completed successfully");
    }

    private ApiResponse<String> completeSubGoal(StageGoalDTO stageGoalDTO) {
        if (stageGoalDTO.getStatus() == null || stageGoalDTO.getStatus() != COMPLETED_STATUS) {
            return ApiResponse.fail(400, "完成操作的 status 必须为 3");
        }

        SubGoalPO existSubGoal = goalMapper.selectSubGoalById(stageGoalDTO.getId());
        if (existSubGoal == null) {
            return ApiResponse.updateFail();
        }
        if (existSubGoal.getStatus() == null || existSubGoal.getStatus() != COMPLETED_STATUS) {
            SubGoalPO completedSubGoal = SubGoalPO.builder()
                    .id(existSubGoal.getId())
                    .stageId(existSubGoal.getStageId())
                    .status(COMPLETED_STATUS)
                    .completedTime(LocalDateTime.now())
                    .build();
            if (goalMapper.updateSubGoalById(completedSubGoal) <= 0) {
                return ApiResponse.updateFail();
            }
        }

        List<SubGoalPO> siblingGoals = goalMapper.selectSubGoalsByStageId(existSubGoal.getStageId());
        boolean allCompleted = !siblingGoals.isEmpty()
                && siblingGoals.stream().allMatch(item -> item.getStatus() != null && item.getStatus() == COMPLETED_STATUS);
        if (allCompleted) {
            StageGoalPO completedStageGoal = StageGoalPO.builder()
                    .id(existSubGoal.getStageId())
                    .status(COMPLETED_STATUS)
                    .completedTime(LocalDateTime.now())
                    .build();
            if (goalMapper.updateStageGoalById(completedStageGoal) <= 0) {
                throw new IllegalStateException("failed to complete stage goal");
            }
        }
        return ApiResponse.success(allCompleted
                ? "sub goal and stage goal completed successfully"
                : "sub goal completed successfully");
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
        if (fallbackUserId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        return fallbackUserId;
    }

    private LocalDateTime defaultEndTime(LocalDateTime endTime) {
        return endTime == null ? LocalDateTime.now().plusDays(DEFAULT_DDL_DAYS) : endTime;
    }
}
