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
    public String getUniversalNutrientContent(Object data) {
        return """
                这是一次餐饮记录，包含若干顿饮食，也包括饮品等。我会向你传递食材、佐料、油量等的克重信息以及烹饪方式。
                请你针对我提供的每一组数据，分别分析其对应的营养成分。
                分析内容仅包含：糖类、蛋白质、脂肪、钠、碳水化合物的含量，并给出每种营养成分的具体克数以及该份食物的总热量。
                请务必以JSON数组格式返回结果，数组中的每个元素是一个JSON对象，对应一张图片的分析结果。
                返回格式严格如下：
                [
                {"uniqueId": "xxx", "卡路里": "xx kcal", "蛋白质": "xx g", "脂肪": "xx g", "碳水化合物": "xx g", "糖": "xx g", "钠": "xx mg"},
                {"uniqueId": "xxx", "卡路里": "xx kcal", "蛋白质": "xx g", "脂肪": "xx g", "碳水化合物": "xx g", "糖": "xx g", "钠": "xx mg"}
                ]
                注意：
                1. 数组长度必须与传入的数据组数一致，且每个对象的uniqueId字段必须与传入数据的uniqueId对应。
                2. 不要遗漏传入的任何一组数据。
                3. 请务必以纯净的 JSON 数组格式返回，严禁包含 ```json、Markdown或任何额外说明文字。
                以下是我的数据组：
               """ + data;
    }

    public List<Map<String, Object>> getNutrientContentByModelQwenPlusAndFlash(Object data) {
        List<Map<String, Object>> contentList = new ArrayList<>();
        contentList.add(Collections.singletonMap("text", """
                你是一个专业的营养学AI助手。我将给你一组饮食数据进行分析。
                我会向你传递食材、佐料、油量等的克重信息以及烹饪方式。
                请你针对我提供的每一组数据，分别分析其对应的营养成分。
                分析内容仅包含：糖类、蛋白质、脂肪、钠、碳水化合物的含量，并给出每种营养成分的具体克数以及该份食物的总热量。
                请务必以JSON数组格式返回结果。
                返回格式严格如下：
                {"universalId": "xxx", "卡路里": "xx kcal", "蛋白质": "xx g", "脂肪": "xx g", "碳水化合物": "xx g", "糖": "xx g", "钠": "xx mg"}
                注意：
                1. 请务必以纯净的 JSON 数组格式返回，严禁包含 ```json、Markdown或任何额外说明文字。
                以下是我的数据组：""" + data));
        return contentList;
    }
}
