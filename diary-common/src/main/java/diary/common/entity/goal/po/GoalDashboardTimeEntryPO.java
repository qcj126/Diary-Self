package diary.common.entity.goal.po;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 目标仪表盘每日投入统计投影。
 */
@Data
public class GoalDashboardTimeEntryPO {
    /** 打卡日期。 */
    private LocalDate checkinDate;
    /** 当日投入时长。 */
    private BigDecimal spentHours;
}
