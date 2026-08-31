package diary.common.entity.ai.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTaskPO {
    private Long id;                        // AI 任务唯一 ID
    private Long userId;                    // JWT认证用户ID，用于任务数据归属校验
    private String clientRequestId;         // 提交幂等键
    private String requestHash;             // 规范化请求内容 SHA-256；防止同一幂等键复用于不同请求
    private String taskType;                // QWEN_PLUS_NUTRIENT
    private String status;                  // 任务状态
    private String inputSnapshot;           // 稳定的任务输入 JSON，或输入表引用
    private Integer attemptCount;           // 成功抢占并进入模型调用流程的次数；不是 MQ delivery 次数
    private Integer maxAttempts;            // 任务允许的最大执行次数
    private Integer recoveryCount;           // 等待态消息补发次数；与模型执行次数分开计数
    private String workerId;                // 当前处理任务的实例
    private LocalDateTime leaseUntil;       // RUNNING 任务租约截止时间
    private Long aiInfoId;                  // 成功后关联现有 AI 信息记录
    private String errorCode;               // 稳定错误码
    private String errorMessage;            // 截断后的错误摘要
    private LocalDateTime createTime;       // 创建时间
    private LocalDateTime queueTime;        // 消息成功发送时间
    private LocalDateTime startTime;        // 开始执行时间
    private LocalDateTime finishTime;       // 执行结束时间
    private LocalDateTime updateTime;       // 最近一次状态更新时间；用于识别长期卡住的非 RUNNING 任务
    private Integer versionId;              // 乐观锁版本：每次有效状态迁移由数据库执行 version_id + 1
}
