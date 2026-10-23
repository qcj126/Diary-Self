package diary.diaryai.service;

import diary.diaryai.entity.ai.dto.LoveRecordSentimentInputSnapshotDTO;

public interface InvokeLoveRecordAIService {
    Integer getCode();

    void getLoveRecordAiResultAndSave(
            LoveRecordSentimentInputSnapshotDTO input,
            Long taskId,
            Long userId,
            String clientRequestId,
            String workerId,
            Integer versionId
    );
}
