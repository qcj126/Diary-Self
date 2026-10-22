package diary.diaryai.impl;

import diary.common.exception.NullResultException;
import diary.diaryai.entity.ai.convert.ConvertPoToVo;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.entity.ai.vo.AiTaskResultVo;
import diary.diaryai.entity.ai.vo.AiTaskStatusVo;
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
        AiTaskResultVo aiTaskResultVo = diaryAiMapper.selectAiTaskResult(taskId, userId);
        if (aiTaskResultVo == null) throw new NullResultException("查询的AI任务结果不存在");
        return aiTaskResultVo;
    }

    @Override
    public List<AiTaskResultVo> getAiTaskList(Long userId, String taskStatus, Integer applicationCode, String title, Integer pageSize, Integer pageNum) {
        return diaryAiMapper.selectAiTaskList(userId, taskStatus, applicationCode, title, pageSize, pageNum * pageSize);
    }
}
