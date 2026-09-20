package diary.common.entity.goal.po;

import lombok.Data;

/**
 * 目标仪表盘到期情况统计投影。
 */
@Data
public class GoalDashboardGoalStatsPO {
    /** 周期内已到期目标数。 */
    private Long dueGoalCount;
    /** 周期内已完成目标数。 */
    private Long completedGoalCount;
    /** 周期内逾期目标数。 */
    private Long overdueGoalCount;
}
