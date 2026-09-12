package diary.diaryai.service;

import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;

import java.util.List;

public interface AiTaskQueryService {
    AiTaskStatusVo getTaskStatus(Long taskId, Long userId);

    AiTaskResultVo getTaskResult(Long taskId, Long userId);

    List<AiTaskResultVo> getAiTaskList(Long userId, String taskStatus, Integer applicationCode, String title, Integer pageSize, Integer pageNum);
}
