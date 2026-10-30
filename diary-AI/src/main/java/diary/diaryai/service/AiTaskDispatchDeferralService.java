package diary.diaryai.service;

import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.enums.aienum.AiTaskErrorCodeEnum;

import java.time.LocalDateTime;

public interface AiTaskDispatchDeferralService {
    boolean deferForGlobalModelPermit(
            AiTaskPO task,
            AiTaskErrorCodeEnum errorCode,
            String errorMessage,
            LocalDateTime nextRetryTime
    );
}
