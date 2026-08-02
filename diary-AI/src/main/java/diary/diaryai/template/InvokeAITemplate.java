package diary.diaryai.template;


import org.springframework.stereotype.Component;

/**
 * 调用AI的模板方法
 */
@Component
public abstract class InvokeAITemplate {
    // 构建提示词
    public abstract Object buildPrompt(Object data);

    // 构建调用请求体
    public abstract Object invokeAi(Object prompt, String model);

    // 解析AI返回结果
    public abstract Object extractResult(Object aiResult, String model, Object prompt);
}
