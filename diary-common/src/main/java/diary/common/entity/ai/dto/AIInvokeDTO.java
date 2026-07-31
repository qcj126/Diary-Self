package diary.common.entity.ai.dto;

import diary.common.entity.ai.ao.ImageIdUrl;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AIInvokeDTO {
    private Integer aiType;                       // 调用AI的枚举值：1-deepseek, 2-通义千问, 3-豆包, 4-元宝
    private Map<Long, String> imageIdUrls;       // 图片的ossUrl，用于下载图片到本地
    private Integer aiApplication;                       // AI用途
}
