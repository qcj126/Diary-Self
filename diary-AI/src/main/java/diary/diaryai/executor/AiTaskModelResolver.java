package diary.diaryai.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.enums.aienum.AIEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiTaskModelResolver {
    private final ObjectMapper objectMapper;

    public AIEnum resolve(AiTaskPO task) {
        try {
            if (task == null) throw new IllegalArgumentException("AI任务数据为空");
            String inputSnapshot = task.getInputSnapshot();
            if (inputSnapshot == null || inputSnapshot.isBlank()) throw new IllegalArgumentException("AI任务快照数据为空, taskId为：" + task.getId());
            JsonNode jsonNode = objectMapper.readTree(inputSnapshot);
            if (jsonNode == null || !jsonNode.isObject()) {
                throw new IllegalArgumentException("AI任务快照不是合法 JSON 对象, taskId=" + task.getId());
            }

            JsonNode aiTypeNode = jsonNode.get("aiType");
            if (aiTypeNode == null || !aiTypeNode.isIntegralNumber()) {
                throw new IllegalArgumentException("AI任务快照缺少合法 aiType, taskId=" + task.getId());
            }
            return AIEnum.fromCode(aiTypeNode.intValue());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("AI任务快照 JSON 解析失败, taskId=" + task.getId(), e);
        }
    }
}
