package diary.diaryai.service;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.po.AiTaskPO;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;

import java.util.Map;

public interface AiTaskCommandService {
    AiTaskPO createTaskAndOutbox(AiInvokeDTO request, Long userId);

    void processData(Long taskId, Object data, String model, Map<String, String> result, Double temperature, Long userId, String workerId, Integer versionId);

    ConsumeResult handleExecutionFailure(AiTaskMessageDto message, AiTaskPO claimedTask, String workerId, Exception executionException);
}
