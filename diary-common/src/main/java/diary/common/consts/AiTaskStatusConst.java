package diary.common.consts;

public class AiTaskStatusConst {
    public static final String PENDING = "PENDING";	// 数据库已创建任务，消息尚未确认发送成功
    public static final String QUEUED = "QUEUED";	// 任务消息已发送到 RocketMQ
    public static final String RUNNING = "RUNNING";	//   某个消费者正在执行 Qwen Plus 调用
    public static final String RETRY_WAIT = "RETRY_WAIT";	// 本次执行发生可重试错误，等待再次投递
    public static final String SUCCESS = "SUCCESS";	// 模型结果已经可靠保存
    public static final String FAILED = "FAILED";	// 永久错误或达到最大任务尝试次数
    public static final String DEAD_LETTER = "DEAD_LETTER";	// 消息进入死信，需要人工检查
}
