package diary.diaryai.service;

import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;

public interface AiTaskQueryService {
    AiTaskStatusVo getTaskStatus(Long taskId, Long userId);

    AiTaskResultVo getTaskResult(Long taskId, Long userId);
}
