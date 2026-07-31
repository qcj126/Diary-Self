package diary.diaryai.strategy.service;

/**
 * 调用AI的顶级接口
 */
public interface InvokeAIService {
    void invokeAI(Object data, Integer aiApplication, Integer aiType);
    Integer getCode();
}
