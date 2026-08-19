package diary.diaryai.strategy.nutrientanlalyze;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import diary.common.enums.aienum.AIEnum;
import diary.common.exception.CustomException;
import diary.diaryai.prompt.PromptContext;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.repository.DatabaseServiceImpl;
import diary.diaryai.service.AiTaskCommandService;
import diary.diaryai.strategy.service.InvokeAIService;
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
public class InvokeQwenPlus extends InvokeAITemplate implements InvokeAIService {
    private final AliCloudProperty aliCloudProperty;
    private final PromptContext promptContext;
    private final MultiModalConversation conv = new MultiModalConversation();
    private final AiTaskCommandService aiTaskCommandService;
    @Override
    public void getAiResultAndSave(Object data, Long taskId, Long userId, String workerId, Integer versionId) {
        String model = aliCloudProperty.getQwenPlusModel();
        Double temperature = aliCloudProperty.getTemperature();
        Object prompt = buildPrompt(data);
        MultiModalConversationResult aiResult = invokeAi(prompt, model);
        Map<String, String> result = extractResult(aiResult, model, prompt);
        validateResult(result);
        /*
         * 以前生产日志打印完整 AI 结果和用户食材，可能暴露用户饮食内容。现在只记录任务及返回字段，
         * 既能排查响应契约，也避免把完整输入输出写入日志。
         */
        log.info("AI nutrient result parsed, taskId={}, fields={}", taskId, result.keySet());
        aiTaskCommandService.processData(taskId, data, model, result, temperature, userId, workerId, versionId);
    }

    @Override
    public Integer getCode() {
        return AIEnum.QWENPLUS.getCode();
    }

    @Override
    public List<Map<String, Object>> buildPrompt(Object data) {
        /*
         * 以前 Qwen Plus 与旧图片/多结果 Prompt 共用方法，Prompt 要求模型返回 universalId，
         * 但模型不知道真实业务 ID，容易生成错误关联。现在使用单业务对象、单 JSON 汇总结果契约。
         */
        return promptContext.getNutrientSummaryContentForQwenPlus(data);
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

    private void validateResult(Map<String, String> result) {
        if (result == null) {
            throw new CustomException("Qwen Plus 返回空的营养分析结果");
        }
        List<String> requiredFields = List.of("卡路里", "蛋白质", "脂肪", "碳水化合物", "糖", "钠");
        for (String field : requiredFields) {
            String value = result.get(field);
            if (value == null || value.isBlank()) {
                throw new CustomException("Qwen Plus 营养分析结果缺少字段: " + field);
            }
        }
    }
}
