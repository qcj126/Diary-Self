package diary.common.enums.aienum;

import lombok.Getter;

@Getter
public enum AiApplicationEnum {
    NUTRITION_ANALYSIS(1, "营养分析"),
    SENTIMENT_ANALYSIS(2, "感情分析"),
    EMOTION_ANALYSIS(3, "情绪解析"),
    DIET_ANALYSIS(4, "饮食分析"),
    BILL_ANALYSIS(5, "账单分析"),
    WHETHER_ANALYSIS(6, "天气分析"),
    DIET_RECOMMENDATION(7, "饮食推荐");
    private final Integer code;
    private final String description;

    AiApplicationEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static void isTrueApplication(Integer code) {
        for (AiApplicationEnum aiApplicationEnum : AiApplicationEnum.values()) {
            if (aiApplicationEnum.getCode().equals(code)) {
                return;
            }
        }
        throw new RuntimeException("没有在本系统找到对应的ai应用方向");
    }
}
