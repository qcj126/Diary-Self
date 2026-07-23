package diary.diarygoal.impl.query;

import diary.common.convert.goal.POConvertToVO;
import diary.common.entity.goal.dto.GoalQueryDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.entity.goal.vo.StageGoalVO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.query.GoalQueryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoalQueryServiceImpl implements GoalQueryService {
    @Resource
    private GoalMapper goalMapper;

    @Override
    public ApiResponse<StageGoalVO> getGoalById(Long id) {
        if (id == null) {
            throw new ParamIllegalException("goal id cannot be empty");
        }
        StageGoalPO stageGoalPO = goalMapper.selectStageGoalById(id);
        if (stageGoalPO == null) {
            throw new ParamIllegalException("goal does not exist");
        }
        return ApiResponse.success(
                POConvertToVO.convertToStageGoalVO(stageGoalPO, goalMapper.selectSubGoalsByStageId(id))
        );
    }

    @Override
    public ApiResponse<List<StageGoalVO>> queryGoals(GoalQueryDTO goalQueryDTO) {
        List<StageGoalPO> stageGoalPOList = goalMapper.selectStageGoals();
        List<Long> stageIds = stageGoalPOList.stream().map(StageGoalPO::getId).toList();
        Map<Long, List<SubGoalPO>> subGoalMap = stageIds.isEmpty()
                ? Map.of()
                : goalMapper.selectSubGoalsByStageIds(stageIds).stream()
                .collect(Collectors.groupingBy(SubGoalPO::getStageId));

        List<StageGoalVO> stageGoalVOList = stageGoalPOList.stream()
                .map(stageGoalPO -> POConvertToVO.convertToStageGoalVO(
                        stageGoalPO, subGoalMap.getOrDefault(stageGoalPO.getId(), List.of())
                ))
                .toList();
        return ApiResponse.success(stageGoalVOList);
    }
}
