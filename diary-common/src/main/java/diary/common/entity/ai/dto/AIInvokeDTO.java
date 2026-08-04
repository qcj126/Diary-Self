package diary.common.entity.ai.dto;

import diary.common.entity.ai.ao.ImageIdUrl;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AIInvokeDTO {
    private Integer aiType;                       // 调用AI的枚举值：1-deepseek, 2-通义千问, 3-豆包, 4-元宝
    private List<Map<String, String>> materials;  // 食材列表
    private Integer aiApplication;                // AI用途
    private String flag;                          // 标记: 饮食或食谱，对应id为universalId
}
