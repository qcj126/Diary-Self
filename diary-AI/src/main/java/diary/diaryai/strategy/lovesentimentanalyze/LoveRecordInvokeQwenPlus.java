package diary.diaryai.strategy.lovesentimentanalyze;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
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
import diary.diaryai.strategy.UsualMethod;
import diary.diaryai.template.InvokeAITemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class LoveRecordInvokeQwenPlus extends InvokeAITemplate implements InvokeLoveRecordAIService {
    private final AliCloudProperty aliCloudProperty;
    private final PromptContext promptContext;
    private final MultiModalConversation conv = new MultiModalConversation();
    private final AiTransactionService aiTransactionService;

    @Override
    public void getLoveRecordAiResultAndSave(LoveRecordSentimentInputSnapshotDTO data, Long taskId,
            Long userId, String clientRequestId, String workerId, Integer versionId) {
        String model = aliCloudProperty.getQwenPlusModel();
        Double temperature = aliCloudProperty.getTemperature();
        Object prompt = buildPrompt(data);
        MultiModalConversationResult aiResult = invokeAi(prompt, model);
        Map<String, String> result = extractResult(aiResult, model, prompt);
        UsualMethod.validateLoveRecordResult(result);
        log.info("AI love-record sentiment result parsed, taskId={}, fields={}", taskId, result.keySet());
        aiTransactionService.processLoveRecordSentimentData(taskId, data.getAiType(), model, result,
                temperature, userId, clientRequestId, workerId, versionId);
    }

    @Override
    public Integer getCode() {
        return AIEnum.QWEN37PLUS.getCode();
    }

    @Override
    public List<Map<String, Object>> buildPrompt(Object data) {
        return promptContext.getLoveRecordSummaryContentForQwenPlus(data);
    }

    @Override
    public MultiModalConversationResult invokeAi(Object prompt, String model) {
        /**
         * 纯文本任务仍使用多模态调用类
         * 当前没有图片输入，仍使用 MultiModalConversation。如果 Qwen Plus 的普通文本接口满足需求，建议换成文本 Generation API；暂时继续使用也能接受，但命名和 Prompt 不应再体现图片或多模态业务。
         */
        Constants.baseHttpApiUrl = aliCloudProperty.getUrl();
        String apiKey = aliCloudProperty.getApiKey();
        Double temperature = aliCloudProperty.getTemperature();
        try {
            List<Map<String, Object>> userMsg = (List<Map<String, Object>>) prompt;
            MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                    .content(userMsg).build();

            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .temperature(temperature.floatValue())
                    .messages(Collections.singletonList(userMessage))
                    .build();
            return conv.call(param);
        } catch (Exception e) {
            throw new CustomException("调用Qwen3.7-Plus模型失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, String> extractResult(Object aiResult, String model, Object prompt) {
        try {
            String aiContent = ((MultiModalConversationResult) aiResult)
                    .getOutput()
                    .getChoices()
                    .getFirst()
                    .getMessage()
                    .getContent()
                    .getFirst()
                    .get("text")
                    .toString();
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            return gson.fromJson(aiContent, type);
        } catch (RuntimeException responseException) {
            throw new CustomException("Qwen Plus 响应不是约定的单个 JSON 对象: " + responseException.getMessage());
        }
    }
}
