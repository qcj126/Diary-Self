package diary.diaryai.recovery;

import diary.common.entity.ai.po.AiTaskPO;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.redis.AiTaskCacheService;
import diary.diaryai.service.AiTaskRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiTaskRecoveryJob {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskRecoveryService aiTaskRecoveryService;
    private final AiTaskCacheService taskCache;

    @Scheduled(fixedDelayString = "${diary.ai.task.recovery-interval-ms:30000}")
    public void recoverExpiredRunning() {
        List<AiTaskPO> tasks = diaryAiMapper.selectExpiredRunningTasks(
                LocalDateTime.now(), 50);

        for (AiTaskPO task : tasks) {
            try {
                aiTaskRecoveryService.recover(task);
                taskCache.evict(task.getId());
            } catch (RuntimeException e) {
                log.error("恢复AI任务失败, taskId={}", task.getId(), e);
            }
        }
    }
}
