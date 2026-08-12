package diary.common.entity.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiInvokeDTO {
    private String clientRequestId;                             // 客户端请求ID    防止用户重复点击导致重复创建任务
    private Integer aiType;                                     // 调用AI的枚举值
    private Map<String, Map<String, String>> materials;         // 食材列表
    private Integer aiApplication;                              // AI用途
    private String flag;                                        // 标记: 饮食或食谱，对应id为universalId
    private Long universalId;                                   // 标记: 饮食或食谱，对应id为universalId
}
