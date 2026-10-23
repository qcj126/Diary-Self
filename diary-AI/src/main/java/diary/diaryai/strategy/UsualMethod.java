package diary.diaryai.strategy;

import diary.common.exception.CustomException;

import java.util.List;
import java.util.Map;

public class UsualMethod {
    public static void validateNutrientResult(Map<String, String> result) {
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

    public static void validateLoveRecordResult(Map<String, String> result) {
        if (result == null) {
            throw new CustomException("Qwen Plus 返回空的爱情情感分析结果");
        }

        List<String> requiredFields = List.of(
                "sentiment", "score", "summary", "suggestion"
        );

        for (String field : requiredFields) {
            String value = result.get(field);
            if (value == null || value.isBlank()) {
                throw new CustomException("爱情情感分析结果缺少字段: " + field);
            }
        }

        String sentiment = result.get("sentiment").trim();
        if (!List.of("POSITIVE", "NEUTRAL", "NEGATIVE", "MIXED").contains(sentiment)) {
            throw new CustomException("爱情情感分析 sentiment 不合法: " + sentiment);
        }

        try {
            int score = Integer.parseInt(result.get("score").trim());
            if (score < -100 || score > 100) {
                throw new CustomException("爱情情感分析 score 必须在 -100 到 100 之间");
            }
        } catch (NumberFormatException e) {
            throw new CustomException("爱情情感分析 score 必须是整数");
        }

        if (result.get("summary").trim().length() > 200) {
            throw new CustomException("爱情情感分析 summary 超过 200 字");
        }

        if (result.get("suggestion").trim().length() > 200) {
            throw new CustomException("爱情情感分析 suggestion 超过 200 字");
        }
    }
}
