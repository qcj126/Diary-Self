package diary.diaryai.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.diaryai.factory.AIFactory;
import diary.diaryai.mapper.DiaryAIMapper;
import diary.diaryai.strategy.service.InvokeAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskExecutor {
    private final DiaryAIMapper diaryAIMapper;
    private final AIFactory aiFactory;
    private final ObjectMapper objectMapper;
    public void execute(AiTaskMessageDto message) {
        // 校验message信息

        AiTaskPO aiTaskPO = diaryAIMapper.selectAiTaskByTaskId(message.getTaskId());
        if (aiTaskPO == null) {
            throw new RuntimeException("任务不存在: " + message.getTaskId());
        }
        // 获取任务状态  SUCCESS或FAILED重复消息直接返回
        if (aiTaskPO.getStatus().equals("SUCCESS") || aiTaskPO.getStatus().equals("FAILED")) {
            return;
        }

        // 更新任务状态为RUNNING  添加更多查询条件，锁定单条任务数据
        /**
         * 注释中的原子抢占尚未真正实现
         * [AiTaskExecutor.java (line 34)](E:/Diary-Self/diary-AI/src/main/java/diary/diaryai/executor/AiTaskExecutor.java:34) 已注释“添加更多查询条件，锁定单条任务数据”，方向正确，但当前仍是普通状态更新。
         * Mapper 后续必须做条件更新：
         * WHERE task_id = ?
         *   AND (
         *     status IN ('QUEUED', 'RETRY_WAIT')
         *     OR (status = 'RUNNING' AND lease_until < NOW())
         *   )
         *   AND attempt_count < max_attempts
         * 同时更新 workerId、leaseUntil、attemptCount 和版本号。
         */
        int cnt = diaryAIMapper.updateAiTaskStatus(message.getTaskId(), "RUNNING", null);
        if (cnt < 1) {
            log.info("更新任务状态为RUNNING失败: {}", message.getTaskId());
            return;
        }

        // 获取前端传入的原始数据
        try {
            AiInvokeDTO aiInvokeDTO = objectMapper.readValue(aiTaskPO.getInputSnapshot(), AiInvokeDTO.class);
            InvokeAIService aiService = aiFactory.getAIService(aiInvokeDTO.getAiType());
            aiService.getAiResultAndSave(aiInvokeDTO.getMaterials(), aiInvokeDTO.getAiApplication(), aiInvokeDTO.getAiType(), aiInvokeDTO.getFlag(), message.getTaskId(), aiInvokeDTO.getUniversalId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
