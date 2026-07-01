package diary.diarygoal.impl.query;

import diary.common.convert.goal.POConvertToVO;
import diary.common.entity.goal.dto.GoalQueryDTO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.vo.StageGoalVO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.query.GoalQueryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return ApiResponse.success(POConvertToVO.convertToStageGoalVO(
                stageGoalPO,
                goalMapper.selectSubGoalsByStageGoalId(id)
        ));
    }

    @Override
    public ApiResponse<List<StageGoalVO>> queryGoals(GoalQueryDTO goalQueryDTO) {
        GoalQueryDTO queryDTO = goalQueryDTO == null ? new GoalQueryDTO() : goalQueryDTO;
        List<StageGoalPO> stageGoalPOList = goalMapper.selectStageGoals(queryDTO);
        List<StageGoalVO> stageGoalVOList = stageGoalPOList.stream()
                .map(stageGoalPO -> POConvertToVO.convertToStageGoalVO(
                        stageGoalPO,
                        goalMapper.selectSubGoalsByStageGoalId(stageGoalPO.getId())
                ))
                .toList();
        return ApiResponse.success(stageGoalVOList);
    }

    @Override
    public StageGoalDTO queryExportData(Integer lastDays, Integer exportSize) {
        // TODO 从数据库中查数据，封装到 StageGoalDTO 中
        return new StageGoalDTO();
    }
}
