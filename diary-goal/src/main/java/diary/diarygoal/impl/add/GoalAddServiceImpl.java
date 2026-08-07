package diary.diarygoal.impl.add;

import diary.common.convert.goal.DTOConvertToPO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.add.GoalAddService;
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
public class GoalAddServiceImpl implements GoalAddService {
    private static final Long DEFAULT_USER_ID = 10000L;
    private static final long DEFAULT_DDL_DAYS = 7L;

    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addGoal(StageGoalDTO stageGoalDTO) {
        try {
            validateStageGoal(stageGoalDTO);

            Long stageGoalId = MyUtils.getPrimaryKey();
            StageGoalPO stageGoalPO = DTOConvertToPO.stageGoalDTOConvertToStageGoalPO(stageGoalDTO);
            stageGoalPO.setId(stageGoalId);
            stageGoalPO.setUserId(resolveUserId(stageGoalPO.getUserId()));
            stageGoalPO.setEndTime(defaultEndTime(stageGoalPO.getEndTime()));

            if (goalMapper.insertStageGoal(stageGoalPO) <= 0) {
                return ApiResponse.addFail();
            }

            List<SubGoalPO> subGoalPOS = buildSubGoalPOS(stageGoalDTO.getSubGoals(), stageGoalId, stageGoalPO.getUserId());
            if (!subGoalPOS.isEmpty() && goalMapper.batchInsertSubGoal(subGoalPOS) != subGoalPOS.size()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return ApiResponse.addFail();
            }

            return ApiResponse.success("goal added successfully");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.addFail();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> batchAddSubGoal(List<SubGoalDTO> subGoalDTOList) {
        try {
            MyUtils.check().notNull(subGoalDTOList, "subGoals").listNotEmpty(subGoalDTOList, "subGoals");

            List<SubGoalPO> subGoalPOS = new ArrayList<>();
            for (SubGoalDTO subGoalDTO : subGoalDTOList) {
                validateSubGoal(subGoalDTO);
                SubGoalPO subGoalPO = DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoalDTO);
                MyUtils.check().notNull(subGoalPO.getStageId(), "stageId");
                subGoalPO.setId(MyUtils.getPrimaryKey());
                subGoalPO.setUserId(resolveUserId(subGoalPO.getUserId()));
                subGoalPO.setEndTime(defaultEndTime(subGoalPO.getEndTime()));
                subGoalPOS.add(subGoalPO);
            }

            if (goalMapper.batchInsertSubGoal(subGoalPOS) != subGoalPOS.size()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return ApiResponse.addFail();
            }

            return ApiResponse.success("sub goals added successfully");
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ApiResponse.addFail();
        }
    }

    private void validateStageGoal(StageGoalDTO stageGoalDTO) {
        MyUtils.check()
                .notNull(stageGoalDTO, "stageGoal")
                .notEmpty(stageGoalDTO.getTitle(), "title")
                .notEmpty(stageGoalDTO.getCategory(), "category")
                .notEmpty(stageGoalDTO.getDescription(), "description");
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

    private List<SubGoalPO> buildSubGoalPOS(List<SubGoalDTO> subGoals, Long stageGoalId, Long userId) {
        List<SubGoalPO> subGoalPOS = new ArrayList<>();
        if (subGoals == null || subGoals.isEmpty()) {
            return subGoalPOS;
        }

        for (SubGoalDTO subGoal : subGoals) {
            validateSubGoal(subGoal);
            SubGoalPO subGoalPO = DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoal);
            subGoalPO.setId(MyUtils.getPrimaryKey());
            subGoalPO.setStageId(stageGoalId);
            subGoalPO.setUserId(resolveUserId(subGoalPO.getUserId(), userId));
            subGoalPO.setEndTime(defaultEndTime(subGoalPO.getEndTime()));
            subGoalPOS.add(subGoalPO);
        }
        return subGoalPOS;
    }

    private Long resolveUserId(Long userId) {
        return userId == null ? DEFAULT_USER_ID : userId;
    }

    private Long resolveUserId(Long userId, Long fallbackUserId) {
        return userId == null ? fallbackUserId : userId;
    }

    private LocalDateTime defaultEndTime(LocalDateTime endTime) {
        return endTime == null ? LocalDateTime.now().plusDays(DEFAULT_DDL_DAYS) : endTime;
    }
}
