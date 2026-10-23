package diary.common.entity.feign;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoveRecordAiSnapshotDTO {
    private Long loveRecordId;

    // 列表展示和 Prompt 上下文
    private String title;

    // 第一版情绪回顾实际分析的正文
    private String content;

    private LocalDate recordDate;

    // DATE / DAILY / TRAVEL / ANNIVERSARY
    private String categoryCode;
}
