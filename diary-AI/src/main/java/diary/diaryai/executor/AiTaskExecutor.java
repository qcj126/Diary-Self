package diary.diaryai.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.dto.AiTaskMessageDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.diaryai.factory.AIFactory;
import diary.diaryai.strategy.service.InvokeAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskExecutor {
    private final AIFactory aiFactory;
    private final ObjectMapper objectMapper;
    public boolean execute(AiTaskMessageDto message, AiTaskPO aiTaskPO) {
        // 在两次查询之间，理论上可能发生：
        // 任务被管理程序或人工删除；
        // 补偿线程改变了任务状态和版本；
        // 其他流程异常修改了记录；
        // execute() 将来被其他代码直接调用，没有经过抢占流程；
        // 查询错误地走了存在延迟的读库。
        // 因此，这里需要进行两次查询，并进行状态检查。
        if (aiTaskPO == null) {
            throw new IllegalArgumentException("任务不存在: " + message.getTaskId());
        }

        try {
            AiInvokeDTO aiInvokeDTO = objectMapper.readValue(aiTaskPO.getInputSnapshot(), AiInvokeDTO.class);
            InvokeAIService aiService = aiFactory.getAIService(aiInvokeDTO.getAiType());
            aiService.getAiResultAndSave(aiInvokeDTO, message.getTaskId(), message.getUserId(), aiTaskPO.getWorkerId(), aiTaskPO.getVersionId());
            return true;
        } catch (JsonProcessingException e) {
            /*
             * input_snapshot 是提交时已经固定的持久化数据，反序列化失败不会通过 MQ 重投自行恢复。
             * 以前统一包装为普通 RuntimeException，容易被当成临时错误反复调用；现在标记为永久参数错误。
             */
            throw new IllegalArgumentException("AI任务输入快照无法反序列化: " + message.getTaskId(), e);
        }
    }
}
