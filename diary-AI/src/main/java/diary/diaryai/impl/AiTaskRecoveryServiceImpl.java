package diary.diaryai.impl;

import diary.common.entity.ai.dto.AiTaskProcessDto;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.enums.aienum.AiTaskErrorCodeEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.service.AiTaskRecoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskRecoveryServiceImpl implements AiTaskRecoveryService {
    private final DiaryAiMapper diaryAiMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recover(AiTaskPO task) {
        LocalDateTime now = LocalDateTime.now();

        // 先看尝试次数是否大于最大可尝试次数
        if (task.getAttemptCount() >= task.getMaxAttempts()) {
            // 记录任务的错误信息
            AiTaskProcessDto failed = AiTaskProcessDto.builder()
                    .taskId(task.getId())
                    .userId(task.getUserId())
                    .clientRequestId(task.getClientRequestId())
                    .versionId(task.getVersionId())
                    .finishTime(now)
                    .errorCode(AiTaskErrorCodeEnum.RETRY_EXHAUSTED.name())
                    .errorMessage("RUNNING租约过期且尝试次数已耗尽")
                    .build();
            // 更新任务的状态为失败
            int cnt = diaryAiMapper.markFailedIfAttemptsExhausted(failed);
            if (cnt == 0) {
                log.warn("任务状态修改为失败未成功，请检查任务ID：{}", task.getId());
            }
            // TODO 向outbox插入失败事件数据
            return;
        }

        // 尝试次数未达到最大可尝试次数，更新任务状态为重试等待
        AiTaskProcessDto retry = AiTaskProcessDto.builder()
                .taskId(task.getId())
                .versionId(task.getVersionId())
                .leaseUntil(now)
                .errorCode(AiTaskErrorCodeEnum.RETRYABLE_ERROR.name())
                .errorMessage("RUNNING租约过期，等待恢复")
                .build();

        int cnt = diaryAiMapper.recoverExpiredRunning(retry);
        if (cnt == 0) {
            log.warn("任务状态修改为重试等待未成功，请检查任务ID：{}", task.getId());
            throw new RuntimeException("任务状态修改为重试等待未成功，请检查任务ID：" + task.getId());
        }

        // TODO 向outbox插入任务恢复事件
        int insertCnt = diaryAiMapper.insertRetryTaskOutbox(task, now);
        if (insertCnt == 0) {
            log.warn("任务恢复事件插入outbox未成功，请检查任务ID：{}", task.getId());
            throw new RuntimeException("任务恢复事件插入outbox未成功，请检查任务ID：" + task.getId());
        }
    }
}
