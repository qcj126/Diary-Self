package diary.diaryai.recovery.listener;

import diary.diaryai.recovery.event.TaskRecoveredEvent;
import diary.diaryai.redis.AiTaskCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class TaskCacheEvictListener {
    private final AiTaskCacheService aiTaskCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTaskStatusChanged(TaskRecoveredEvent event) {
        try {
            aiTaskCacheService.evict(event.getTaskId());
            log.debug("任务状态变更后清理缓存成功, taskId={}", event.getTaskId());
        } catch (Exception e) {
            log.error("任务状态变更后清理缓存失败, taskId={}", event.getTaskId(), e);
            // AFTER_COMMIT 阶段已无法回滚数据库；抛异常只会制造误报，缓存 TTL 会自动收敛。
        }
    }
}
