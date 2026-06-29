package diary.common.entity.goal.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StageGoalPO {
    private Long id;
    private Long userId;
    private String creator;
    private String category;
    private String title;
    private String description;
    private Boolean deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
