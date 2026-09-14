package diary.diaryai.prompt;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class PromptContext {
    public List<Map<String, Object>> getNutrientSummaryContentForQwenPlus(Object data) {
        List<Map<String, Object>> contentList = new ArrayList<>();
        contentList.add(Collections.singletonMap("text", """
                你是一个专业的营养学 AI 助手。请对下面这一份饮食或食谱中的全部食材、佐料、油量和烹饪方式进行汇总分析。
                只返回这一份业务对象的汇总营养结果，不要按食材分别返回多条数据。
                返回内容只包含：卡路里、蛋白质、脂肪、碳水化合物、糖、钠。
                请只返回一个纯净的 JSON 对象，不要返回 JSON 数组、Markdown、代码块或其他说明文字。
                返回格式严格如下：
                {"卡路里": "xx kcal", "蛋白质": "xx g", "脂肪": "xx g", "碳水化合物": "xx g", "糖": "xx g", "钠": "xx mg"}
                不要返回 universalId；业务 ID 由服务端从任务输入快照中读取，不能由模型猜测。
                以下是本次任务输入：
                """ + data));
        return contentList;
    }

    public String getUniversalNutrientContent(Object data) {
        return """
                你是一个专业的营养学 AI 助手。请对下面这一份饮食或食谱中的全部食材、佐料、油量和烹饪方式进行汇总分析。
                只返回这一份业务对象的汇总营养结果，不要按食材分别返回多条数据。
                返回内容只包含：卡路里、蛋白质、脂肪、碳水化合物、糖、钠。
                请只返回一个纯净的 JSON 对象，不要返回 JSON 数组、Markdown、代码块或其他说明文字。
                返回格式严格如下：
                {"卡路里": "xx kcal", "蛋白质": "xx g", "脂肪": "xx g", "碳水化合物": "xx g", "糖": "xx g", "钠": "xx mg"}
                不要返回 universalId；业务 ID 由服务端从任务输入快照中读取，不能由模型猜测。
                以下是本次任务输入：
               """ + data;
    }
}
