package diary.common.entity.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskProcessDto {
    private Long taskId;                            // AI 任务唯一 ID
    private Long userId;                            // JWT认证用户ID，用于状态迁移归属校验
    private String clientRequestId;                 // 提交幂等键
    private String taskType;                        // QWEN_PLUS_NUTRIENT
    private String status;                          // 任务状态
    private String inputSnapshot;                   // 稳定的任务输入 JSON，或输入表引用
    private Integer attemptCount;                   // 成功抢占并进入模型调用流程的次数；不是 MQ delivery 次数
    private Integer maxAttempts;                    // 任务允许的最大执行次数
    private Integer recoveryCount;                  // 等待态消息补发次数
    private String workerId;                        // 当前处理任务的实例
    private LocalDateTime leaseUntil;               // RUNNING 任务租约截止时间
    private Long leaseSeconds;                      // 由数据库当前时间计算租约，避免应用实例时钟偏差
    private Long aiInfoId;                          // 成功后关联现有 AI 信息记录
    private String errorCode;                       // 稳定错误码
    private String errorMessage;                    // 截断后的错误摘要
    private LocalDateTime createTime;               // 创建时间
    private LocalDateTime queueTime;                // 消息成功发送时间
    private LocalDateTime startTime;                // 开始执行时间
    private LocalDateTime finishTime;               // 执行结束时间
    private Integer versionId;                      // 当前 Worker 抢占后持有的预期版本；SQL 成功后由数据库自增
}
