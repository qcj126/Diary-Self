package diary.diaryai.strategy.nutrientanlalyze;

import diary.common.exception.CustomException;

import java.util.List;
import java.util.Map;

public class UsualMethod {
    public static void validateResult(Map<String, String> result) {
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
