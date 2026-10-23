package diary.diaryai.entity.ai.dto;

import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveRecordSentimentInputSnapshotDTO {
    private Integer snapshotVersion;

    // 选择哪个模型
    private Integer aiType;

    // Love 服务读取并确认归属后的真实分析素材
    private LoveRecordAiSnapshotDTO loveRecord;
}
