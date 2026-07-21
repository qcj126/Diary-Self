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
                POConvertToVO.convertToStageGoalVO(stageGoalPO, goalMapper.selectSubGoalsByStageGoalId(id))
        );
    }

    @Override
    public ApiResponse<List<StageGoalVO>> queryGoals(GoalQueryDTO goalQueryDTO) {
        GoalQueryDTO queryDTO = goalQueryDTO == null ? new GoalQueryDTO() : goalQueryDTO;
        List<StageGoalPO> stageGoalPOList = goalMapper.selectStageGoals(queryDTO);
        List<Long> stageGoalIds = stageGoalPOList.stream().map(StageGoalPO::getId).toList();
        Map<Long, List<SubGoalPO>> subGoalMap = stageGoalIds.isEmpty()
                ? Map.of()
                : goalMapper.selectSubGoalsByStageGoalIds(stageGoalIds).stream()
                .collect(Collectors.groupingBy(SubGoalPO::getStageGoalId));

        List<StageGoalVO> stageGoalVOList = stageGoalPOList.stream()
                .map(stageGoalPO -> POConvertToVO.convertToStageGoalVO(
                        stageGoalPO,
                        subGoalMap.getOrDefault(stageGoalPO.getId(), List.of())
                ))
                .toList();
        return ApiResponse.success(stageGoalVOList);
    }
}
