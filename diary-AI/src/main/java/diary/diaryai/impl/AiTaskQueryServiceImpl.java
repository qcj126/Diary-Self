package diary.diaryai.impl;

import diary.common.exception.NullResultException;
import diary.common.exception.ParamIllegalException;
import diary.diaryai.entity.ai.convert.ConvertPoToVo;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.entity.ai.vo.AiTaskResultVo;
import diary.diaryai.entity.ai.vo.AiSentimentTaskResultVo;
import diary.diaryai.entity.ai.vo.AiTaskStatusVo;
import diary.diaryai.entity.ai.vo.AiTaskSummaryVo;
import diary.diaryai.enums.aienum.AiTaskTypeEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.service.AiTaskQueryService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTaskQueryServiceImpl implements AiTaskQueryService {
    private static final int MAX_PAGE_SIZE = 100;

    private final DiaryAiMapper diaryAiMapper;

    @Override
    public AiTaskStatusVo getNutrientTaskStatus(Long taskId, Long userId) {
        return ConvertPoToVo.convertToVo(getNutrientTask(taskId, userId));
    }

    @Override
    public AiTaskStatusVo getLoveRecordSentimentTaskStatus(Long taskId, Long userId) {
        return ConvertPoToVo.convertToVo(getLoveRecordSentimentTask(taskId, userId));
    }

    @Override
    public AiTaskResultVo getNutrientTaskResult(Long taskId, Long userId) {
        getNutrientTask(taskId, userId);
        AiTaskResultVo aiTaskResultVo = diaryAiMapper.selectNutrientTaskResult(taskId, userId);
        if (aiTaskResultVo == null) throw new NullResultException("查询的AI任务结果不存在");
        return aiTaskResultVo;
    }

    @Override
    public AiSentimentTaskResultVo getLoveRecordSentimentTaskResult(Long taskId, Long userId) {
        getLoveRecordSentimentTask(taskId, userId);
        AiSentimentTaskResultVo result = diaryAiMapper.selectLoveRecordSentimentTaskResult(taskId, userId);
        if (result == null) {
            throw new NullResultException("查询的AI恋爱记录情感分析任务结果不存在");
        }
        return result;
    }

    @Override
    public List<AiTaskSummaryVo> getAiTaskSummaryList(Long userId, String taskStatus, Integer applicationCode,
                                                       String title, Integer pageSize, Integer pageNum) {
        MyUtils.check()
                .notNull(userId, "userId")
                .notNull(pageSize, "pageSize")
                .notNull(pageNum, "pageNum");
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ParamIllegalException("pageSize 必须在 1 到 " + MAX_PAGE_SIZE + " 之间");
        }
        if (pageNum < 0) {
            throw new ParamIllegalException("pageNum 不能小于 0");
        }

        long offset = Math.multiplyExact((long) pageNum, pageSize);
        if (offset > Integer.MAX_VALUE) {
            throw new ParamIllegalException("分页偏移量超出允许范围");
        }
        return diaryAiMapper.selectAiTaskSummaryList(
                userId, taskStatus, applicationCode, title, pageSize, (int) offset);
    }

    private AiTaskPO getLoveRecordSentimentTask(Long taskId, Long userId) {
        MyUtils.check()
                .notNull(taskId, "taskId")
                .notNull(userId, "userId");
        AiTaskPO task = diaryAiMapper.selectAiTaskByTaskIdAndUserId(taskId, userId);
        if (task == null) {
            throw new NullResultException("AI任务不存在: " + taskId);
        }
        if (!AiTaskTypeEnum.LOVE_RECORD_SENTIMENT_ANALYSIS.name().equals(task.getTaskType())) {
            throw new ParamIllegalException("该任务不是恋爱记录情感分析任务: " + taskId);
        }
        return task;
    }

    private AiTaskPO getNutrientTask(Long taskId, Long userId) {
        MyUtils.check()
                .notNull(taskId, "taskId")
                .notNull(userId, "userId");
        AiTaskPO task = diaryAiMapper.selectAiTaskByTaskIdAndUserId(taskId, userId);
        if (task == null) {
            throw new NullResultException("AI任务不存在: " + taskId);
        }
        String taskType = task.getTaskType();
        if (!AiTaskTypeEnum.RECIPE_NUTRIENT_ANALYSIS.name().equals(taskType)
                && !AiTaskTypeEnum.DIET_NUTRIENT_ANALYSIS.name().equals(taskType)) {
            throw new ParamIllegalException("该任务不是营养分析任务: " + taskId);
        }
        return task;
    }
}
