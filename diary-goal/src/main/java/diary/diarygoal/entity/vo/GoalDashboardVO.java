package diary.diarygoal.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 目标数据仪表盘与周期复盘结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalDashboardVO {
    /** 周期类型：1 周报，2 月报。 */
    private Integer periodType;
    /** 周期名称。 */
    private String periodName;
    /** 统计周期开始日期。 */
    private LocalDate startDate;
    /** 统计周期结束日期。 */
    private LocalDate endDate;
    /** 本次统计实际覆盖到的日期，当前周期不会包含未来数据。 */
    private LocalDate dataThroughDate;
    /** 核心统计摘要。 */
    private Summary summary;
    /** 投入时长趋势；周报按日，月报按自然周。 */
    private List<TimeTrendPoint> timeTrend;
    /** 分类投入占比。 */
    private List<CategoryShare> categoryDistribution;
    /** 目标燃尽数据。 */
    private List<BurndownPoint> burndown;
    /** 长期未更新且尚未结束的目标。 */
    private List<StaleGoal> staleGoals;

    /**
     * 周期复盘核心指标。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        /** 周期内已到期目标数。 */
        private Long dueGoalCount;
        /** 周期内已到期且已完成的目标数。 */
        private Long completedGoalCount;
        /** 周期内逾期完成或截至统计日仍未完成的目标数。 */
        private Long overdueGoalCount;
        /** 目标完成率，单位为百分比。 */
        private BigDecimal completionRate;
        /** 目标逾期率，单位为百分比。 */
        private BigDecimal overdueRate;
        /** 周期内总投入时长，单位为小时。 */
        private BigDecimal periodSpentHours;
        /** 截至统计日的连续打卡天数。 */
        private Integer currentStreakDays;
        /** 周期内最长连续打卡天数。 */
        private Integer longestStreakDays;
        /** 周期内完成目标的预计时长，单位为小时。 */
        private BigDecimal completedEstimatedHours;
        /** 周期内完成目标的实际时长，单位为小时。 */
        private BigDecimal completedActualHours;
        /** 实际时长减预计时长，单位为小时。 */
        private BigDecimal hourDeviation;
        /** 实际时长相对预计时长的偏差率，单位为百分比。 */
        private BigDecimal hourDeviationRate;
    }

    /**
     * 投入时长趋势点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeTrendPoint {
        /** 趋势点开始日期。 */
        private LocalDate startDate;
        /** 趋势点结束日期。 */
        private LocalDate endDate;
        /** 展示标签。 */
        private String label;
        /** 本时间段投入时长，单位为小时。 */
        private BigDecimal spentHours;
    }

    /**
     * 分类投入占比。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryShare {
        /** 目标分类。 */
        private String category;
        /** 分类投入时长，单位为小时。 */
        private BigDecimal spentHours;
        /** 分类投入占比，单位为百分比。 */
        private BigDecimal percentage;
    }

    /**
     * 燃尽图数据点。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BurndownPoint {
        /** 数据日期。 */
        private LocalDate date;
        /** 理想剩余时长，单位为小时。 */
        private BigDecimal idealRemainingHours;
        /** 实际剩余时长，单位为小时。 */
        private BigDecimal actualRemainingHours;
    }

    /**
     * 长期未更新目标。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaleGoal {
        /** 阶段目标 ID。 */
        private Long goalId;
        /** 目标标题。 */
        private String title;
        /** 目标分类。 */
        private String category;
        /** 目标状态编码。 */
        private Integer status;
        /** 目标状态名称。 */
        private String statusName;
        /** 最后活动日期。 */
        private LocalDate lastActivityDate;
        /** 截至统计日未活动天数。 */
        private Long inactiveDays;
    }
}
