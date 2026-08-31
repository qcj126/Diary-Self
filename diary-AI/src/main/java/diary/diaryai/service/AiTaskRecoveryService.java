package diary.diaryai.service;

import diary.common.entity.ai.po.AiTaskPO;

public interface AiTaskRecoveryService {
    void recover(AiTaskPO task);

    void recoverWaiting(AiTaskPO task);
}
