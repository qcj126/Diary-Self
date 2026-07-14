package diary.diaryai.template;


import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.protocol.Protocol;
import diary.common.exception.CustomException;
import diary.diaryai.properties.AliCloudProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 调用AI的模板方法
 */
@Component
public abstract class InvokeAITemplate {

    private final AliCloudProperty aliCloudProperty;

    protected InvokeAITemplate(AliCloudProperty aliCloudProperty) {
        this.aliCloudProperty = aliCloudProperty;
    }

    // 构建提示词
    public abstract String buildPrompt();

    // 构建调用请求体
    public GenerationResult constructRequest(String prompt, String model) {
        try {
            String url = aliCloudProperty.getUrl();
            String apiKey = aliCloudProperty.getApiKey();
            Double temperature = aliCloudProperty.getTemperature();

            // 通过阿里云百炼平台调用api
            Generation generation = new Generation(Protocol.HTTP.getValue(), url);
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("You are a helpful assistant.")
                    .build();
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();
            GenerationParam param = GenerationParam.builder()
                    .apiKey(System.getenv(apiKey))
                    .model(model)
                    .temperature(temperature.floatValue())
                    .messages(Arrays.asList(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .build();
            return generation.call(param);
        } catch (Exception e) {
            throw new CustomException("调用AI失败: " + e);
        }
    }
    // 解析AI返回结果
    public abstract Object extractResult(GenerationResult aiResult);
}
