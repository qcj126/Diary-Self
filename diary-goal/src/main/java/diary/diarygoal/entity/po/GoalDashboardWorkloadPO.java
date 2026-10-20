package diary.diarygoal.entity.po;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 目标仪表盘工时统计投影。
 */
@Data
public class GoalDashboardWorkloadPO {
    /** 燃尽范围内目标的预计总时长。 */
    private BigDecimal totalEstimatedHours;
    /** 统计周期开始前已经投入的时长。 */
    private BigDecimal investedBeforePeriodHours;
    /** 周期内完成目标的预计时长。 */
    private BigDecimal completedEstimatedHours;
    /** 周期内完成目标的实际时长。 */
    private BigDecimal completedActualHours;
}
