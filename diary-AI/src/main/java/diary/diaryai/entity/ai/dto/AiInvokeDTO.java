package diary.diaryai.entity.ai.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AiInvokeDTO {
    private String clientRequestId;                             // 客户端请求ID    防止用户重复点击导致重复创建任务
    private Integer aiType;                                     // 调用AI的枚举值
    private Map<String, String> ingredients;                    // 食材及用量
    private String cookWay;                                     // 烹饪方式
    private String flag;                                        // 标记: 饮食或食谱，对应id为universalId
    private Long universalId;                                   // 标记: 饮食或食谱，对应id为universalId
}
