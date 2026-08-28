package diary.common.entity.log;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperLogPO {
    private Long id;
    private String module;          // 操作模块
    private String description;     // 操作描述
    private String operationType;   // 操作类型
    private String requestUrl;      // 请求URL
    private String requestMethod;   // 请求方式
    private String requestParams;   // 请求参数
    private String responseResult;  // 返回结果
    private String operator;        // 操作人
    private String ip;              // 操作IP
    private Integer costTime;       // 耗时(毫秒)
    private LocalDateTime createTime; // 操作时间
}