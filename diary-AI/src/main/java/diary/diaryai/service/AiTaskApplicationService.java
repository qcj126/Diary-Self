package diary.diaryai.service;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.vo.AiTaskSubmitVo;

public interface AiTaskApplicationService {

    AiTaskSubmitVo submitTask(AiInvokeDTO aiInvokeDTO);

}
