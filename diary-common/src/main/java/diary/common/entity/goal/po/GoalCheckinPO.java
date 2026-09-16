package diary.common.entity.goal.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalCheckinPO {
    private Long id;
    private String requestId;
    private Long userId;
    private Long stageGoalId;
    private Long subGoalId;
    private Integer checkinType;
    private BigDecimal spentHours;
    private String content;
    /** JSON array persisted in MySQL. */
    private String evidenceUrls;
    private LocalDate checkinDate;
    private Integer source;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
