//package diary.diaryai.strategy.nutrientanlalyze;
//
//import com.alibaba.dashscope.aigc.generation.Generation;
//import com.alibaba.dashscope.aigc.generation.GenerationParam;
//import com.alibaba.dashscope.aigc.generation.GenerationResult;
//import com.alibaba.dashscope.common.Message;
//import com.alibaba.dashscope.common.Role;
//import com.alibaba.dashscope.utils.Constants;
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import diary.common.entity.ai.po.AiInfoPO;
//import diary.common.entity.ai.po.AiNutrientPO;
//import diary.common.enums.aienum.AIEnum;
//import diary.common.exception.CustomException;
//import diary.diaryai.mapper.DiaryAiMapper;
//import diary.diaryai.prompt.PromptContext;
//import diary.diaryai.properties.AliCloudProperty;
//import diary.diaryai.strategy.service.InvokeAIService;
//import diary.diaryai.template.InvokeAITemplate;
//import diary.utils.commonutil.MyUtils;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.lang.reflect.Type;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Component
//@Order(1)
//@RequiredArgsConstructor
//public class InvokeDeepSeekV4Pro extends InvokeAITemplate implements InvokeAIService {
//    private final AliCloudProperty aliCloudProperty;
//    private final PromptContext promptContext;
//    private final DiaryAiMapper diaryAiMapper;
//    private final Generation generation = new Generation();
//
//    @Override
//    public void getAiResultAndSave(Object data, Long taskId, Long userId) {
//        String model = aliCloudProperty.getDeepSeekV4FlashModel();
//        Object prompt = buildPrompt(data);
//        AiInfoPO aiInfoPO = AiInfoPO.builder()
//                .id(MyUtils.getPrimaryKey())
//                .userId(10000L)
//                .temperature(aliCloudProperty.getTemperature().toString())
//                .model(model)
//                .aiType(aiType)
//                .aiApplication(aiApplication)
//                .build();
//        diaryAiMapper.insertAiInfo(aiInfoPO);
//        GenerationResult aiResult = invokeAi(prompt, model);
//        List<Map<String, String>> resultList = extractResult(aiResult, model, prompt);
//        log.info("AI返回的结果列表： {}", resultList);
//        List<AiNutrientPO> aiNutrientPOS = new ArrayList<>();
//        for (Map<String, String> result : resultList) {
//            aiNutrientPOS.add(AiNutrientPO.builder()
//                    .id(MyUtils.getPrimaryKey())
//                    .userId(10000L)
//                    .universalId(Long.parseLong(result.get("imageId")))
//                    .aiInfoId(aiInfoPO.getId())
//                    .calory(result.get("卡路里"))
//                    .protein(result.get("蛋白质"))
//                    .fat(result.get("脂肪"))
//                    .carbohydrate(result.get("碳水化合物"))
//                    .sugar(result.get("糖"))
//                    .sodium(result.get("钠"))
//                    .build());
//        }
//        diaryAiMapper.insertAiNutrient(aiNutrientPOS);
//    }
//
//    @Override
//    public Integer getCode() {
//        return AIEnum.DEEPSEEK.getCode();
//    }
//
//    @Override
//    public String buildPrompt(Object data) {
//        return promptContext.getUniversalNutrientContent(data);
//    }
//
//    @Override
//    public GenerationResult invokeAi(Object prompt, String model) {
//        try {
//            Constants.baseHttpApiUrl = aliCloudProperty.getUrl();
//            String apiKey = aliCloudProperty.getApiKey();
//            Double temperature = aliCloudProperty.getTemperature();
//
//            // 通过阿里云百炼平台调用api
//            Message systemMsg = Message.builder()
//                    .role(Role.SYSTEM.getValue())
//                    .content("你是一个营养分析专家，专门帮顾客分析食物的营养成分和健康价值。")
//                    .build();
//            Message userMsg = Message.builder()
//                    .role(Role.USER.getValue())
//                    .content(prompt.toString())
//                    .build();
//            GenerationParam param = GenerationParam.builder()
//                    .apiKey(apiKey)
//                    .model(model)
//                    .temperature(temperature.floatValue())
//                    .messages(Arrays.asList(systemMsg, userMsg))
//                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
//                    .build();
//            return generation.call(param);
//        } catch (Exception e) {
//            throw new CustomException("调用Qwen3.7-max失败: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public List<Map<String, String>> extractResult(Object aiResult, String model, Object prompt) {
//        String aiContent = ((GenerationResult) aiResult).getOutput().getChoices().getFirst().getMessage().getContent();
//        if (aiContent == null || aiContent.contains("无法识别")) {
//            aiResult = invokeAi(prompt, model);
//            aiContent = ((GenerationResult) aiResult).getOutput().getChoices().getFirst().getMessage().getContent();
//        }
//        if (aiContent == null || aiContent.contains("无法识别")) {
//            throw new CustomException("AI返回的结果无法识别，请检查提示词或数据格式。");
//        }
//        Gson gson = new Gson();
//        Type type = new TypeToken<List<Map<String, String>>>(){}.getType();
//        return gson.fromJson(aiContent, type);
//    }
//}
