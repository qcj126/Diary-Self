package diary.diaryai.strategy.service;

/**
 * 调用AI的顶级接口
 */
public interface InvokeAIService {
    Object invokeAI(Object data);
    Integer getCode();
}
