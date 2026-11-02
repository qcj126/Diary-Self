package diary.diaryai.exceptions;

/**
 * DashScope 熔断器处于 OPEN 状态时抛出。
 *
 * <p>这是可重试的基础设施异常。Consumer 已有的失败状态机将任务转为
 * {@code RETRY_WAIT}，并交由 RocketMQ 的既有重投策略再次尝试。</p>
 */
public class AiProviderCircuitOpenException extends RuntimeException {

    public AiProviderCircuitOpenException(String model, Throwable cause) {
        super("AI 模型调用熔断中，model=" + model, cause);
    }
}
