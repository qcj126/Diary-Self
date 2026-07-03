package diary.diaryai.template;

/**
 * 调用AI的模板方法
 */
public abstract class InvokeAITemplate {
    // 构建提示词
    public abstract String buildPrompt();
    // 构建调用请求体
    public abstract Object constructRequest(String prompt);
    // 调用AI
    public abstract Object callAI(Object data);
    // 解析AI返回结果
    public abstract Object extractResult(Object aiResult);
}
