package diary.diaryai.strategy.lovesentimentanalyze;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import diary.common.exception.CustomException;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentInputSnapshotDTO;
import diary.diaryai.enums.aienum.AIEnum;
import diary.diaryai.prompt.PromptContext;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.service.AiTransactionService;
import diary.diaryai.service.InvokeLoveRecordAIService;
import diary.diaryai.guard.AiModelCallGuard;
import diary.diaryai.strategy.UsualMethod;
import diary.diaryai.template.InvokeAITemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@Order(1)
@RequiredArgsConstructor
public class LoveRecordInvokeDeepseekPro extends InvokeAITemplate implements InvokeLoveRecordAIService {
    private final AliCloudProperty aliCloudProperty;
    private final PromptContext promptContext;
    private final Generation gen = new Generation();
    private final AiTransactionService aiTransactionService;
    private final AiModelCallGuard aiModelCallGuard;

    @Override
    public void getLoveRecordAiResultAndSave(LoveRecordSentimentInputSnapshotDTO data, Long taskId,
            Long userId, String clientRequestId, String workerId, Integer versionId) {
        String model = aliCloudProperty.getDeepSeekV4ProModel();
        Double temperature = aliCloudProperty.getTemperature();
        Object prompt = buildPrompt(data);
        GenerationResult aiResult = invokeAi(prompt, model);
        Map<String, String> result = extractResult(aiResult, model, prompt);
        UsualMethod.validateLoveRecordResult(result);
        log.info("DeepSeek love-record sentiment result parsed, taskId={}, fields={}", taskId, result.keySet());
        aiTransactionService.processLoveRecordSentimentData(taskId, data.getAiType(), model, result,
                temperature, userId, clientRequestId, workerId, versionId);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DEEPSEEKV4PRO0813.getCode();
    }

    @Override
    public String buildPrompt(Object data) {
        return promptContext.getLoveRecordSummaryText(data);
    }

    @Override
    public GenerationResult invokeAi(Object prompt, String model) {
        Constants.baseHttpApiUrl = aliCloudProperty.getUrl();
        String apiKey = aliCloudProperty.getApiKey();
        Double temperature = aliCloudProperty.getTemperature();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一名温和、客观的恋爱记录情感分析助手。请严格遵守用户消息中的 JSON 输出契约。")
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content((String) prompt)
                .build();
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .temperature(temperature.floatValue())
                .messages(List.of(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .enableThinking(true)
                .build();
        // 受检 SDK 异常由 AiModelCallGuard 统一转换并交给任务状态机重试。
        return aiModelCallGuard.execute(model, () -> gen.call(param));
    }

    @Override
    public Map<String, String> extractResult(Object aiResult, String model, Object prompt) {
        try {
            String content = ((GenerationResult) aiResult)
                    .getOutput()
                    .getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent();
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            return gson.fromJson(content, type);
        } catch (RuntimeException responseException) {
            throw new CustomException("DeepSeek 响应不是约定的单个 JSON 对象: " + responseException.getMessage());
        }
    }
}
