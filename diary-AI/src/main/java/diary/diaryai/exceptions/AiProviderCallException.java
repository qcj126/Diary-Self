package diary.diaryai.exceptions;

/**
 * DashScope SDK 在发起远程调用前或调用期间抛出的受检异常。
 *
 * <p>该异常保持为可重试的运行时异常，使 Consumer 能沿用既有的任务重试状态机，
 * 同时保留原始异常作为 cause 供日志与排障使用。</p>
 */
public class AiProviderCallException extends RuntimeException {

    public AiProviderCallException(String model, Exception cause) {
        super("调用 AI 模型失败，model=" + model, cause);
    }
}
