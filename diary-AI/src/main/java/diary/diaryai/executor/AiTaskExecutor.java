package diary.diaryai.executor;

import diary.common.exception.ParamIllegalException;
import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.enums.aienum.AiTaskTypeEnum;
import diary.diaryai.service.AiTaskProcessorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaskExecutor {
    private final AiTaskProcessorService aiTaskProcessorService;
    public void execute(AiTaskMessageDto message, AiTaskPO aiTaskPO) {
        if (aiTaskPO == null) throw new IllegalArgumentException("任务不存在: " + message.getTaskId());

        String taskType = aiTaskPO.getTaskType();

        if (taskType.equals(AiTaskTypeEnum.DIET_NUTRIENT_ANALYSIS.name()) || taskType.equals(AiTaskTypeEnum.RECIPE_NUTRIENT_ANALYSIS.name())) {
            aiTaskProcessorService.NutrientTaskProcessor(aiTaskPO.getInputSnapshot(), message, aiTaskPO);
        } else if (taskType.equals(AiTaskTypeEnum.LOVE_RECORD_SENTIMENT_ANALYSIS.name())) {
            aiTaskProcessorService.SentimentTaskProcessor(aiTaskPO.getInputSnapshot(), message, aiTaskPO);
        } else {
            throw new ParamIllegalException("不支持的AiFlag类型");
        }
    }
}
