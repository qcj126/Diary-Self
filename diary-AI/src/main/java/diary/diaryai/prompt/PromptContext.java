package diary.diaryai.prompt;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageItemImage;
import com.alibaba.dashscope.embeddings.MultiModalEmbeddingItemImage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PromptContext {
    public String getNutrientContentByModelQwenMax(Object data) {
        return "这是一次餐饮记录，包含多张图片（每张图片可能对应一道菜、一个餐品或一餐的整体视图），" +
                "请你针对我提供的每一张图片，分别分析其对应的食物营养成分。" +
                "分析内容仅包含：糖类、蛋白质、脂肪、钠、碳水化合物的含量，并给出每种营养成分的具体克数以及该份食物的总热量。" +
                "请务必以JSON数组格式返回结果，数组中的每个元素是一个JSON对象，对应一张图片的分析结果。" +
                "返回格式严格如下：[" +
                "{\"imageId\": \"图片id\", \"卡路里\": \"xx kcal\", \"蛋白质\": \"xx g\", \"脂肪\": \"xx g\", \"碳水化合物\": \"xx g\", \"糖\": \"xx g\", \"钠\": \"xx mg\"}," +
                "{\"imageId\": \"图片id\", \"卡路里\": \"xx kcal\", \"蛋白质\": \"xx g\", \"脂肪\": \"xx g\", \"碳水化合物\": \"xx g\", \"糖\": \"xx g\", \"钠\": \"xx mg\"}" +
                "]。" +
                "注意：" +
                "1. 数组长度必须与传入的图片数量一致，且每个对象的imageId字段必须与传入的图片id对应。" +
                "2. 如果某张图片无法识别或分析，请在对应对象中注明，但不要遗漏任何一张图片。" +
                "3. 只返回JSON数组，不要包含任何额外的说明文字。" +
                "我的图片数据，是map格式，键为图片id，值为图片url：" + data;
    }

    public List<Map<String, Object>> getNutrientContentByModelQwenPlus(Object data) {
        List<Map<String, Object>> contentList = new ArrayList<>();
        Map<Long, String> dataMap = (Map<Long, String>) data;
        contentList.add(Collections.singletonMap("text", "你是一个专业的营养学AI助手。我将给你多张餐食图片进行分析。\n" +
                "你需要在分析营养数据的同时，处理**元数据映射**。\n" +
                "\n" +
                "【输入映射说明】\n" +
                "本次传入的图片顺序及对应的 ObjectKey 如下：\n" +
                "ObjectKey 1: food_path_1785514820831微信图片_20260801001510_3_2.jpg\n" +
                "ObjectKey 2: food_path_1785514675026微信图片_20260801001509_2_2.jpg\n" +
                "\n" +
                "【你的任务】\n" +
                "请严格按顺序对每一张图片进行独立的营养成分分析。\n" +
                "分析内容必须包含：总热量(kcal)、蛋白质(g)、脂肪(g)、碳水化合物(g)、糖(g)、钠(mg)。\n" +
                "\n" +
                "【输出格式 - 最关键约束】\n" +
                "请务必以纯净的 JSON 数组格式返回，严禁包含 ```json、Markdown或任何额外说明文字。\n" +
                "数组的长度必须为 2（与图片数量一致）。每个元素必须严格遵循以下结构：\n" +
                "\n" +
                "{\n" +
                "  \"objectKey\": \"这里填入该图片对应的 ObjectKey，必须原样复制上方给出的文字，不可捏造，不可更改\",\n" +
                "  \"卡路里\": \"xx kcal\",\n" +
                "  \"蛋白质\": \"xx g\",\n" +
                "  \"脂肪\": \"xx g\",\n" +
                "  \"碳水化合物\": \"xx g\",\n" +
                "  \"糖\": \"xx g\",\n" +
                "  \"钠\": \"xx mg\"\n" +
                "}\n" +
                "\n" +
                "【兜底规则】\n" +
                "如果某张图片内容过于模糊或无法识别，返回数据格式必须如下：\n" +
                "{\n" +
                "  \"无法识别\": \"这里填入该图片对应的 ObjectKey，必须原样复制上方给出的文字，不可捏造，不可更改\"" +
                "}" +
                "\n" +
                "\n" +
                "现在开始分析，直接输出 JSON。"));
        for (Map.Entry<Long, String> entry : dataMap.entrySet()) {
            HashMap<String, Object> imageInfoMap = new HashMap<>();
            imageInfoMap.put("image", String.valueOf(entry.getValue()));
            contentList.add(imageInfoMap);
        }
        return contentList;
    }
}
