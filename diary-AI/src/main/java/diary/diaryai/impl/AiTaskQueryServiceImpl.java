package diary.diaryai.impl;

import diary.common.convert.ai.ConvertPoToVo;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.redis.AiTaskCacheService;
import diary.diaryai.service.AiTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AiTaskQueryServiceImpl implements AiTaskQueryService {
    private final DiaryAiMapper diaryAiMapper;
    private final AiTaskCacheService taskCache;

    @Override
    public AiTaskStatusVo getTaskStatus(Long taskId) {
        Long userId = 10000L;
        Optional<AiTaskStatusVo> cached = taskCache.get(taskId, userId);
        if (cached.isPresent()) {
            return cached.get();
        }

        AiTaskPO aiTaskPO = diaryAiMapper.selectAiTaskByTaskIdAndUserId(taskId, userId);
        if (aiTaskPO == null) {
            throw new IllegalArgumentException("AI任务不存在: " + taskId);
        }

        AiTaskStatusVo result = ConvertPoToVo.convertToVo(aiTaskPO);
        taskCache.put(result, userId);
        return result;
    }

    @Override
    public AiTaskResultVo getTaskResult(Long taskId) {
        Long userId = 10000L;
        AiTaskPO aiTaskPO = diaryAiMapper.selectAiTaskByTaskIdAndUserId(taskId, userId);
        if (aiTaskPO == null) {
            throw new IllegalArgumentException("AI任务不存在: " + taskId);
        }

        if (!AiTaskStatusEnum.SUCCESS.name().equals(aiTaskPO.getStatus())) {
            return AiTaskResultVo.builder()
                    .taskId(taskId)
                    .status(aiTaskPO.getStatus())
                    .errorCode(aiTaskPO.getErrorCode())
                    .errorMessage(aiTaskPO.getErrorMessage())
                    .build();
        }

        AiNutrientPO aiNutrientPO = diaryAiMapper.selectAiNutrientByTaskId(taskId);
        if (aiNutrientPO == null) {
            throw new IllegalStateException("SUCCESS任务缺少营养结果: " + taskId);
        }

        return ConvertPoToVo.convertToVo(aiNutrientPO, aiTaskPO);
    }
}
