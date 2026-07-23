package diary.common.convert.goal;

import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.entity.goal.vo.StageGoalVO;
import diary.common.entity.goal.vo.SubGoalVO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public class POConvertToVO {
    public static StageGoalVO convertToStageGoalVO(StageGoalPO po, List<SubGoalPO> subGoalPOList) {
        if (po == null) {
            return null;
        }
        List<SubGoalPO> safeSubGoals = subGoalPOList == null ? Collections.emptyList() : subGoalPOList;
        BigDecimal learnedHours = safeSubGoals.stream()
                .map(SubGoalPO::getLearnedHours)
                .map(POConvertToVO::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal estimatedHours = safeSubGoals.stream()
                .map(SubGoalPO::getEstimatedHours)
                .map(POConvertToVO::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return StageGoalVO.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .category(po.getCategory())
                .title(po.getTitle())
                .description(po.getDescription())
                .learnedHours(learnedHours)
                .estimatedHours(estimatedHours)
                .remainingHours(nonNegative(estimatedHours.subtract(learnedHours)))
                .progress(calcProgress(learnedHours, estimatedHours))
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .daysSinceUpdate(calcDaysSinceUpdate(po.getUpdateTime()))
                .subGoals(safeSubGoals.stream().map(POConvertToVO::convertToSubGoalVO).toList())
                .build();
    }

    public static SubGoalVO convertToSubGoalVO(SubGoalPO po) {
        if (po == null) {
            return null;
        }
        BigDecimal learnedHours = zeroIfNull(po.getLearnedHours());
        BigDecimal estimatedHours = zeroIfNull(po.getEstimatedHours());
        return SubGoalVO.builder()
                .id(po.getId())
                .stageGoalId(po.getStageId())
                .userId(po.getUserId())
                .title(po.getTitle())
                .content(po.getContent())
                .learnedHours(learnedHours)
                .estimatedHours(estimatedHours)
                .remainingHours(nonNegative(estimatedHours.subtract(learnedHours)))
                .progress(calcProgress(learnedHours, estimatedHours))
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private static Integer calcProgress(BigDecimal learnedHours, BigDecimal estimatedHours) {
        if (estimatedHours == null || estimatedHours.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return learnedHours.multiply(BigDecimal.valueOf(100))
                .divide(estimatedHours, 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100))
                .intValue();
    }

    private static Long calcDaysSinceUpdate(LocalDateTime updateTime) {
        if (updateTime == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(updateTime.toLocalDate(), LocalDateTime.now().toLocalDate());
    }
}
