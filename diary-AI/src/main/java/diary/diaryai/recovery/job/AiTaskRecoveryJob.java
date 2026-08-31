package diary.diaryai.recovery.job;

import diary.common.entity.ai.po.AiTaskPO;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.properties.AiTaskProperties;
import diary.diaryai.service.AiTaskRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiTaskRecoveryJob {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskRecoveryService aiTaskRecoveryService;
    private final AiTaskProperties aiTaskProperties;

    @Scheduled(fixedDelayString = "${diary.ai.task.recovery-interval-ms:30000}")
    public void recoverExpiredRunning() {
        List<AiTaskPO> tasks = diaryAiMapper.selectExpiredRunningTasks(
                aiTaskProperties.getTask().getRecoveryBatchSize());

        for (AiTaskPO task : tasks) {
            try {
                // 由于在这个方法中，都处于MySQL事务范围内，所以需要写一个编程式事务进行缓存的处理
                aiTaskRecoveryService.recover(task);
            } catch (RuntimeException e) {
                log.error("恢复AI任务失败, taskId={}", task.getId(), e);
            }
        }

        /*
         * 改前：只扫描租约过期的 RUNNING，消息在真正执行前进入 DLQ 时没有任何恢复入口。
         * 改后：同一轮任务再扫描长期停留的 PENDING/QUEUED/RETRY_WAIT，由服务判断是否存在活跃 Outbox 后恢复。
         */
        List<AiTaskPO> waitingTasks = diaryAiMapper.selectStaleWaitingTasks(
                aiTaskProperties.getTask().getWaitingRecoverySeconds(),
                aiTaskProperties.getTask().getWaitingMaxRecoveryMessages(),
                aiTaskProperties.getTask().getRecoveryBatchSize());
        for (AiTaskPO task : waitingTasks) {
            try {
                aiTaskRecoveryService.recoverWaiting(task);
            } catch (RuntimeException e) {
                log.error("恢复等待态AI任务失败, taskId={}", task.getId(), e);
            }
        }
    }
}
