package diary.diarygoal.entity.po;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 目标仪表盘分类投入统计投影。
 */
@Data
public class GoalDashboardCategoryHoursPO {
    /** 目标分类。 */
    private String category;
    /** 分类投入时长。 */
    private BigDecimal spentHours;
}
