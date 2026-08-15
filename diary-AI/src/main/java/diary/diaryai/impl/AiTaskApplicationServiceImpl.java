package diary.diaryai.impl;

import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.ai.vo.AiTaskSubmitVo;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.service.AiTaskApplicationService;
import diary.diaryai.service.AiTaskCommandService;
import diary.utils.commonutil.MyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskApplicationServiceImpl implements AiTaskApplicationService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskCommandService aiTaskCommandService;

    @Override
    public AiTaskSubmitVo submitTask(AiInvokeDTO aiInvokeDTO) {
        validateAndNormalizeRequest(aiInvokeDTO);

        final Long userId = 10000L;
        // 已存在的任务直接返回，未创建的任务继续处理
        AiTaskPO existingTask = diaryAiMapper.selectByUserIdAndClientRequestId(userId, aiInvokeDTO.getClientRequestId());
        if (existingTask != null) {
            return toSubmitVo(existingTask, "该请求已提交");
        }
        AiTaskPO aiTaskPO;
        try {
            aiTaskPO = aiTaskCommandService.createTaskAndOutbox(aiInvokeDTO, userId);
            if (aiTaskPO == null) {
                throw new IllegalStateException("AI任务创建失败");
            }
        } catch (DuplicateKeyException duplicateKeyException) {
            // aiTask表有userId和clientRequestId联合唯一索引
            // 当并发请求同时插入时，后插入的请求会抛出 DuplicateKeyException
            AiTaskPO concurrentTask = diaryAiMapper.selectByUserIdAndClientRequestId(userId, aiInvokeDTO.getClientRequestId());
            if (concurrentTask == null) {
                // 两个不同请求在极短时间到达，taskId生成相同，但是userId或clientRequestId不同，那么就查不到数据
                // 此时就不是联合唯一索引冲突，而是主键冲突了
                throw duplicateKeyException;
            }
            return toSubmitVo(concurrentTask, "该请求已提交");
        }
        return toSubmitVo(aiTaskPO, "AI分析任务正在处理中");
    }

    private void validateAndNormalizeRequest(AiInvokeDTO request) {
        MyUtils.check()
                .notNull(request, "aiInvokeDTO")
                .notEmpty(request.getClientRequestId(), "clientRequestId")
                .notNull(request.getAiType(), "aiType")
                .notNull(request.getAiApplication(), "aiApplication")
                .notEmpty(request.getFlag(), "flag")
                .notNull(request.getMaterials(), "materials")
                .stringKeyMapNotContainsEmpty(request.getMaterials(), "materials")
                .notNull(request.getUniversalId(), "universalId");

        request.setClientRequestId(request.getClientRequestId().trim());
        request.setFlag(request.getFlag().trim().toUpperCase());
    }

    private AiTaskSubmitVo toSubmitVo(AiTaskPO task, String message) {
        return AiTaskSubmitVo.builder()
                .taskId(task.getId())
                .status(task.getStatus())
                .message(message)
                .build();
    }
}
