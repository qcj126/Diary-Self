package diary.diaryai.impl;

import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;
import diary.diaryai.service.AiTaskQueryService;
import org.springframework.stereotype.Service;

@Service
public class AiTaskQueryServiceImpl implements AiTaskQueryService {
    @Override
    public AiTaskStatusVo getTaskStatus(String taskId) {
        return new AiTaskStatusVo();
    }

    @Override
    public AiTaskResultVo getTaskResult(String taskId) {
        return new AiTaskResultVo();
    }
}
