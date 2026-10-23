package diary.diaryai.service;

import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.dto.AiTaskMessageDto;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentInputSnapshotDTO;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentSubmitDTO;
import diary.diaryai.entity.ai.po.AiTaskPO;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;

import java.util.Map;

public interface AiTransactionService {
    AiTaskPO createNutrientTaskAndOutbox(AiInvokeDTO request, Long userId, String recipeTitle);

    void processNutrientData(Long taskId, Object data, String model, Map<String, String> result,
                     Double temperature, Long userId, String workerId, Integer versionId);

    void processLoveRecordSentimentData(Long taskId, Integer aitype, String model,
            Map<String, String> result, Double temperature, Long userId, String clientRequestId, String workerId, Integer versionId);

    ConsumeResult handleExecutionFailure(AiTaskMessageDto message, AiTaskPO claimedTask, String workerId, Exception executionException);

    boolean failExhaustedTask(AiTaskPO task, String errorMessage);

    boolean deadLetterDispatchTask(AiTaskPO task, String errorMessage);

    AiTaskPO createLoveRecordSentimentTaskAndOutbox(LoveRecordAiSnapshotDTO loveRecordAiSnapshotDTO, Long userId, LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO);
}
