package diary.common.entity.diet.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 饮食记录列表查询条件。
 */
@Data
public class DietQueryDTO {
    private Long userId;
    private String keyword;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Byte mealType;
    private String location;
}
