package diary.diaryai.strategy.impl;

import diary.common.enums.aienum.AIEnum;
import diary.diaryai.properties.DeepSeekProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import diary.diaryai.webclient.WebClientConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class InvokeDeepSeek extends InvokeAITemplate implements InvokeAIService {
    private final DeepSeekProperty deepSeekProperty;
    private final WebClient deepSeekWebClient;
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
    public Mono<String> callAI(Object data) {
        String url = deepSeekProperty.getUrl();
        String apiKey = deepSeekProperty.getKey();
        String model = deepSeekProperty.getModel();
        Double temperature = deepSeekProperty.getTemperature();

        return deepSeekWebClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(data)
                .retrieve()
                .bodyToMono(Map.class) // 将响应解析为 Map
                // 3. 提取并返回 AI 回复的内容
                .map(response -> {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.getFirst();
                        Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                        return message.get("content");
                    }
                    return "No response from AI.";
                });
    }

    @Override
    public Object extractResult(Object aiResult) {

        return null;
    }
}
