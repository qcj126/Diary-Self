package diary.diaryai.strategy.impl;

import diary.common.enums.aienum.AIEnum;
import diary.diaryai.properties.DeepSeekProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvokeDeepSeek extends InvokeAITemplate implements InvokeAIService {
    private final DeepSeekProperty deepSeekProperty;

    @Override
    public Object invokeAI(Object data) {
        String prompt = buildPrompt();
        Object request = constructRequest(prompt);
        Object aiResult = callAI(request);
        return extractResult(aiResult);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DEEPSEEK.getCode();
    }

    @Override
    public String buildPrompt() {
        return null;
    }

    @Override
    public Object constructRequest(String prompt) {
        // 1. 构建请求体 (DeepSeek API 格式)
        return Map.of(
        "model", deepSeekProperty.getModel(), // 或从配置读取
            "messages", List.of(
                Map.of("role", "system", "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", prompt)),
        "stream", false // 非流式响应
        );
    }

    @Override
    public Object callAI(Object data) {
        String url = deepSeekProperty.getUrl();
        String apiKey = deepSeekProperty.getApiKey();
        String model = deepSeekProperty.getModel();
        Double temperature = deepSeekProperty.getTemperature();

        return null;
    }

    @Override
    public Object extractResult(Object aiResult) {
        return null;
    }
}
