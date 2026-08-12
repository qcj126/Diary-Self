package diary.diaryai.strategy.nutrientanlalyze;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import diary.common.entity.ai.po.AiInfoPO;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.enums.aienum.AIEnum;
import diary.common.exception.CustomException;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.prompt.PromptContext;
import diary.diaryai.properties.AliCloudProperty;
import diary.diaryai.strategy.service.InvokeAIService;
import diary.diaryai.template.InvokeAITemplate;
import diary.utils.commonutil.MyUtils;
import diary.utils.redis.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.ArrayList;
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
    private final DiaryAIMapper diaryAIMapper;
    private final MultiModalConversation conv = new MultiModalConversation();

    @Override
    public void getAiResultAndSave(Object data, Integer aiApplication, Integer aiType, String flag, Long taskId, Long universalId) {
        String model = aliCloudProperty.getQwenPlusModel();
        Object prompt = buildPrompt(data);
        MultiModalConversationResult aiResult = invokeAi(prompt, model);
        Map<String, String> result = extractResult(aiResult, model, prompt);
        log.info("AI返回的结果： {}", result);
        processData(aiApplication, aiType, flag, taskId, universalId, model, result);
    }

    /**
     * @Transactional 当前不会生效
     * [InvokeQwenPlus.java (line 54)](E:/Diary-Self/diary-AI/src/main/java/diary/diaryai/strategy/nutrientanlalyze/InvokeQwenPlus.java:54) 的 processData() 是 private，而且由同一个类内部调用。
     * Spring 事务依赖代理，私有方法和类内自调用都不会经过代理。因此目前：
     * insertAiInfo
     * insertAiNutrient
     * update SUCCESS
     * 仍然不是一个事务。
     * 建议把落库部分拆到独立 Bean：
     * @Service
     * @RequiredArgsConstructor
     * public class AiResultPersistenceService {
     *
     *     private final DiaryAIMapper diaryAIMapper;
     *
     *     @Transactional(rollbackFor = Exception.class)
     *     public void saveResult(...) {
     *         // insertAiInfo
     *         // insertAiNutrient
     *         // update task SUCCESS
     *     }
     * }
     * 然后由 InvokeQwenPlus 调用这个 Bean。
     * @param aiApplication
     * @param aiType
     * @param flag
     * @param taskId
     * @param universalId
     * @param model
     * @param result
     */
    @Transactional(rollbackFor = Exception.class)
    private void processData(Integer aiApplication, Integer aiType, String flag, Long taskId, Long universalId, String model, Map<String, String> result) {
        AiInfoPO aiInfoPO = AiInfoPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(10000L)
                .temperature(aliCloudProperty.getTemperature().toString())
                .model(model)
                .aiType(aiType)
                .aiApplication(aiApplication)
                .build();
        diaryAIMapper.insertAiInfo(aiInfoPO);
        AiNutrientPO aiNutrientPO = AiNutrientPO.builder()
                .id(MyUtils.getPrimaryKey())
                .userId(10000L)
                .universalId(universalId)
                .aiInfoId(aiInfoPO.getId())
                .calory(result.get("卡路里"))
                .protein(result.get("蛋白质"))
                .fat(result.get("脂肪"))
                .carbohydrate(result.get("碳水化合物"))
                .sugar(result.get("糖"))
                .sodium(result.get("钠"))
                .flag(flag)
                .build();

        diaryAIMapper.insertAiNutrient(aiNutrientPO);
        diaryAIMapper.updateAiTaskStatus(taskId, "SUCCESS", aiInfoPO.getId());
    }

    @Override
    public Integer getCode() {
        return AIEnum.QWENPLUS.getCode();
    }

    @Override
    public List<Map<String, Object>> buildPrompt(Object data) {
        return promptContext.getNutrientContentByModelQwenPlusAndFlash(data);
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
        String aiContent = ((MultiModalConversationResult) aiResult).getOutput().getChoices().getFirst().getMessage().getContent().getFirst().get("text").toString();
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        return gson.fromJson(aiContent, type);
    }
}
