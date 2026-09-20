package diary.common.entity.goal.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 目标仪表盘周期复盘查询参数。
 */
@Data
public class GoalDashboardQueryDTO {
    /**
     * 周期类型：1 周报，2 月报；默认周报。
     */
    private Integer periodType;

    /**
     * 周期锚点日期；为空时使用当天。
     */
    private LocalDate anchorDate;

    /**
     * 连续多少天无活动后视为长期未更新；默认 7 天。
     */
    private Integer staleDays;
}
