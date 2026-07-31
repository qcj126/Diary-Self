package diary.diaryai.prompt;

import org.springframework.stereotype.Component;

@Component
public class PromptContext {
    public String getNutrientContent(Object data) {
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
                "我的图片文件流数据，是map格式，键为图片id，值为文件流：" + data;
    }
}
