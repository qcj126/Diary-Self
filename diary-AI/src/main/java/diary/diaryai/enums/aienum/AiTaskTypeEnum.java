package diary.diaryai.enums.aienum;

import diary.common.exception.NullResultException;
import lombok.Getter;

@Getter
public enum AiTaskTypeEnum {
    DIET_NUTRIENT_ANALYSIS("餐食营养分析"),
    RECIPE_NUTRIENT_ANALYSIS("食谱营养分析"),
    LOVE_RECORD_SENTIMENT_ANALYSIS("恋爱记录情感分析");

    // 后续规划：
    // LOVE_PERIOD_EMOTION_REVIEW：按近 7/30 天恋爱记录做情绪趋势回顾
    // LOVE_CONFLICT_REFLECTION：针对一条冲突记录做事实、感受、需求复盘

    private final String displayName;

    AiTaskTypeEnum(String displayName) {
        this.displayName = displayName;
    }

    public static boolean isTrueTaskType(String taskType) {
        for (AiTaskTypeEnum aiTaskTypeEnum : AiTaskTypeEnum.values()) {
            if (aiTaskTypeEnum.name().equals(taskType)) {
                return true;
            }
        }
        throw new RuntimeException("没有在本系统找到对应的ai应用方向");
    }

    public static String getAiTaskType(String flag) {
        switch (flag) {
            case "DIET" -> {
                return DIET_NUTRIENT_ANALYSIS.name();
            }
            case "RECIPE" -> {
                return RECIPE_NUTRIENT_ANALYSIS.name();
            }
            default -> throw new NullResultException("没有此AiFlag的数据");
        }
    }
}
