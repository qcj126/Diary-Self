package diary.diaryai.service;

import diary.diaryai.entity.ai.vo.AiTaskResultVo;
import diary.diaryai.entity.ai.vo.AiTaskSummaryVo;
import diary.diaryai.entity.ai.vo.AiTaskStatusVo;
import diary.diaryai.entity.ai.vo.AiSentimentTaskResultVo;

import java.util.List;

public interface AiTaskQueryService {
    AiTaskStatusVo getNutrientTaskStatus(Long taskId, Long userId);

    AiTaskStatusVo getLoveRecordSentimentTaskStatus(Long taskId, Long userId);

    AiTaskResultVo getNutrientTaskResult(Long taskId, Long userId);

    AiSentimentTaskResultVo getLoveRecordSentimentTaskResult(Long taskId, Long userId);

    List<AiTaskSummaryVo> getAiTaskSummaryList(Long userId, String taskStatus, Integer applicationCode,
                                                String title, Integer pageSize, Integer pageNum);
}
