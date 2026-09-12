package diary.diaryai.strategy.nutrientanlalyze;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import diary.common.enums.aienum.AIEnum;
import diary.common.exception.CustomException;
import diary.diaryai.prompt.PromptContext;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.service.AiTaskCommandService;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

@Component
@Slf4j
@Order(1)
@RequiredArgsConstructor
public class InvokeDeepseekPro extends InvokeAITemplate implements InvokeAIService {
    private final AliCloudProperty aliCloudProperty;
    private final PromptContext promptContext;
    private final Generation gen = new Generation();
    private final AiTaskCommandService aiTaskCommandService;
    private static int i = 1;

    @Override
    public void getAiResultAndSave(Object data, Long taskId, Long userId, String workerId, Integer versionId) {
        String model = aliCloudProperty.getDeepSeekV4ProModel();
        Double temperature = aliCloudProperty.getTemperature();
        Object prompt = buildPrompt(data);
        GenerationResult aiResult = invokeAi(prompt, model);
        Map<String, String> result = extractResult(aiResult, model, prompt);
        UsualMethod.validateResult(result);
        /*
         * 以前生产日志打印完整 AI 结果和用户食材，可能暴露用户饮食内容。现在只记录任务及返回字段，
         * 既能排查响应契约，也避免把完整输入输出写入日志。
         */
        log.info("AI nutrient result parsed, taskId={}, fields={}", taskId, result.keySet());
        aiTaskCommandService.processData(taskId, data, model, result, temperature, userId, workerId, versionId);
    }

    @Override
    public Integer getCode() {
        return AIEnum.DEEPSEEKV4PRO0813.getCode();
    }

    @Override
    public String buildPrompt(Object data) {
        return promptContext.getUniversalNutrientContent(data);
    }

    @Override
    public GenerationResult invokeAi(Object prompt, String model) {
        /**
         * 纯文本任务仍使用多模态调用类
         * 当前没有图片输入，仍使用 MultiModalConversation。如果 Qwen Plus 的普通文本接口满足需求，建议换成文本 Generation API；暂时继续使用也能接受，但命名和 Prompt 不应再体现图片或多模态业务。
         */
        Constants.baseHttpApiUrl = aliCloudProperty.getUrl();
        String apiKey = aliCloudProperty.getApiKey();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个专业的营养学 AI 助手。")
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(prompt.toString())
                .build();
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(apiKey)
                .model("deepseek-v4-pro-0813")
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .enableThinking(true)
                .build();
        try {
            return gen.call(param);
        } catch (NoApiKeyException e) {
            throw new CustomException("请配置百炼API Key");
        } catch (InputRequiredException e) {
            throw new CustomException("输入参数不完整");
        }
    }

    @Override
    public Map<String, String> extractResult(Object aiResult, String model, Object prompt) {
        String content = ((GenerationResult) aiResult).getOutput().getChoices().getFirst().getMessage().getContent();
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        return gson.fromJson(content, type);
    }
}
