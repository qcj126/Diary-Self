package diary.diaryai.impl;

import diary.common.convert.ai.ConvertPoToVo;
import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;
import diary.common.enums.aienum.AiApplicationEnum;
import diary.common.enums.aienum.AiTaskStatusEnum;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.service.AiTaskQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTaskQueryServiceImpl implements AiTaskQueryService {
    private final DiaryAiMapper diaryAiMapper;

    @Override
    public AiTaskStatusVo getTaskStatus(Long taskId, Long userId) {
        AiTaskPO aiTaskPO = diaryAiMapper.selectAiTaskByTaskIdAndUserId(taskId, userId);
        if (aiTaskPO == null) {
            throw new IllegalArgumentException("AI任务不存在: " + taskId);
        }

        return ConvertPoToVo.convertToVo(aiTaskPO);
    }

    @Override
    public AiTaskResultVo getTaskResult(Long taskId, Long userId) {
        return diaryAiMapper.selectAiTaskResult(taskId, userId);
    }

    @Override
    public List<AiTaskResultVo> getAiTaskList(Long userId, String taskStatus, Integer applicationCode, String title, Integer pageSize, Integer pageNum) {
        return diaryAiMapper.selectAiTaskList(userId, taskStatus, applicationCode, title, pageSize, pageNum * pageSize);
    }
}
