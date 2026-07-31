package diary.diaryai.template;


import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.Constants;
import diary.common.exception.CustomException;
import diary.diaryai.properties.AliCloudProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 调用AI的模板方法
 */
@Component
public abstract class InvokeAITemplate {

    private final AliCloudProperty aliCloudProperty;
    private final static Generation generation = new Generation();

    protected InvokeAITemplate(AliCloudProperty aliCloudProperty) {
        this.aliCloudProperty = aliCloudProperty;
    }
    static {
        Constants.baseHttpApiUrl="https://llm-wdfzfqz3o6y5lf9d.cn-beijing.maas.aliyuncs.com/api/v1";
    }
    // 构建提示词
    public abstract String buildPrompt(Object data);

    // 构建调用请求体
    public GenerationResult constructRequest(String prompt, String model) {
        try {
//            String url = aliCloudProperty.getUrl();
            String apiKey = aliCloudProperty.getApiKey();
            Double temperature = aliCloudProperty.getTemperature();

            // 通过阿里云百炼平台调用api
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content("你是一个营养分析专家，专门帮顾客分析食物的营养成分和健康价值。")
                    .build();
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(prompt)
                    .build();
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
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
