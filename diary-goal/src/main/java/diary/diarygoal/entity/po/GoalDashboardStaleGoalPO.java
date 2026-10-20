package diary.diarygoal.entity.po;

import lombok.Data;

import java.time.LocalDate;

/**
 * 目标仪表盘长期未更新目标投影。
 */
@Data
public class GoalDashboardStaleGoalPO {
    /** 阶段目标 ID。 */
    private Long goalId;
    /** 目标标题。 */
    private String title;
    /** 目标分类。 */
    private String category;
    /** 目标状态。 */
    private Integer status;
    /** 最后活动日期。 */
    private LocalDate lastActivityDate;
    /** 未活动天数。 */
    private Long inactiveDays;
}
