package diary.diaryai.strategy.impl;

import diary.common.enums.aienum.AIEnum;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(1)
public class InvokeDbao extends InvokeAITemplate implements InvokeAIService {
    @Override
    public Object invokeAI(Object data) {
        String prompt = buildPrompt();
        Object request = constructRequest(prompt);
        Object aiResult = callAI(request);
        return extractResult(aiResult);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DBAO.getCode();
    }

    @Override
    public String buildPrompt() {
        return null;
    }

    @Override
    public Object constructRequest(String prompt) {
        return null;
    }

    @Override
    public Mono<String> callAI(Object data) {
        return null;
    }

    @Override
    public Object extractResult(Object aiResult) {
        return null;
    }
}
