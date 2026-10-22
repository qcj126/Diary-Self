package diary.diaryai.entity.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskResultVo {
    private Long taskId;
    private String status;
    private String title;
    private String targetType;
    private Long targetId;
    private Integer aiApplication;
    private Long aiInfoId;
    private String calory;
    private String protein;
    private String fat;
    private String carbohydrate;
    private String sugar;
    private String sodium;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}
