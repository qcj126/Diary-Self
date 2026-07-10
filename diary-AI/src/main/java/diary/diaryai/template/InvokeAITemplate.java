package diary.diaryai.template;


import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.protocol.Protocol;
import diary.diaryai.properties.AliCloudProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 调用AI的模板方法
 */
@Component
@RequiredArgsConstructor
public abstract class InvokeAITemplate {

    private final AliCloudProperty aliCloudProperty;
    // 构建提示词
    public abstract String buildPrompt();

    // 构建调用请求体
    public Object constructRequest(String prompt, String model) {
        String url = aliCloudProperty.getUrl();
        String apiKey = aliCloudProperty.getApiKey();
        Double temperature = aliCloudProperty.getTemperature();

        // 通过阿里云百炼平台调用api
        Generation generation = new Generation(Protocol.HTTP.getValue(), url);
    }

    // 调用AI
    public abstract Object callAI(Object data);
    // 解析AI返回结果
    public abstract Object extractResult(Object aiResult);
}
