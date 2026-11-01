package diary.common.entity.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量查询饮食最新营养分析结果的请求。
 */
@Data
public class AiNutrientBatchQueryDTO {
    /**
     * 饮食记录主键列表。
     */
    private List<Long> dietIds;
}
