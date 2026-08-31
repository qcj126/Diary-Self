package diary.diaryai.service;

import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;

public interface AiTaskQueryService {
    AiTaskStatusVo getTaskStatus(Long taskId);

    AiTaskResultVo getTaskResult(Long taskId);
}
