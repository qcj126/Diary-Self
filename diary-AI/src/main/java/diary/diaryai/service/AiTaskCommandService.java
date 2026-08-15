package diary.diaryai.service;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.po.AiTaskPO;

public interface AiTaskCommandService {
    AiTaskPO createTaskAndOutbox(AiInvokeDTO request, Long userId);
}
