package diary.notify.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 * 定义系统中所有的通知类型，与消息协议中的 notifyType 字段对应
 *
 * 使用场景：
 *   - 业务模块发送通知到 MQ 时，指定 notifyType 标识通知子类
 *   - NotifyMessageCodec 编解码时，作为消息分类依据
 *   - NotifyHandler 分发处理时，根据 notifyType 执行不同逻辑
 *
 * 扩展方式：
 *   - 新增通知类型时，只需在此枚举中添加新项
 *   - 业务模块引用此枚举，保证类型一致性
 */
@Getter
@AllArgsConstructor
public enum NotifyTypeEnum {

    // ========== 目标模块通知 ==========
    /**
     * 阶段目标到期提醒
     * 来源模块：diary-goal
     * 场景：用户的阶段目标即将到期时触发
     */
    GOAL_DUE("GOAL_DUE", "阶段目标到期提醒"),

    /**
     * 目标进度落后提醒
     * 来源模块：diary-goal
     * 场景：目标进度低于预期时触发
     */
    GOAL_PROGRESS("GOAL_PROGRESS", "目标进度落后提醒"),

    // ========== 饮食模块通知 ==========
    /**
     * 饭点提醒 / 饮食记录提醒
     * 来源模块：diary-diet
     * 场景：到达饭点时间或长时间未记录饮食时触发
     */
    DIET_REMIND("DIET_REMIND", "饭点提醒/饮食记录提醒"),

    // ========== 定时任务通知 ==========
    /**
     * 定时任务完成通知
     * 来源模块：diary-xxljob
     * 场景：XXL-Job 定时任务执行完成后通知
     */
    TASK_COMPLETE("TASK_COMPLETE", "定时任务完成通知"),

    // ========== AI 模块通知 ==========
    /**
     * AI 分析完成通知
     * 来源模块：diary-AI
     * 场景：AI 分析报告生成完成后通知
     */
    AI_COMPLETE("AI_COMPLETE", "AI 分析完成通知"),

    // ========== 文件模块通知 ==========
    /**
     * 文件处理完成通知
     * 来源模块：diary-file
     * 场景：文件上传/处理完成后通知
     */
    FILE_READY("FILE_READY", "文件处理完成通知");

    /**
     * 通知类型编码（与 JSON 协议中的 notifyType 字段值一致）
     */
    private final String code;

    /**
     * 通知类型描述
     */
    private final String description;
}
