package diary.common.entity.goal.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalCheckinVO {
    private Long id;
    private String requestId;
    private Long userId;
    private Long stageGoalId;
    private Long subGoalId;
    private Integer checkinType;
    private String checkinTypeName;
    private BigDecimal spentHours;
    private String content;
    private List<String> evidenceUrls;
    private LocalDate checkinDate;
    private Integer source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
