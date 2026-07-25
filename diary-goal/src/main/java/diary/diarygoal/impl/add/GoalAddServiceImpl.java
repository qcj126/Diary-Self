package diary.diarygoal.impl.add;

import diary.common.convert.goal.DTOConvertToPO;
import diary.common.convert.goal.POConvertToVO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.entity.goal.vo.SubGoalVO;
import diary.common.exception.ParamIllegalException;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.add.GoalAddService;
import diary.utils.commonutil.MyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoalAddServiceImpl implements GoalAddService {
    @Resource
    private GoalMapper goalMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addGoal(StageGoalDTO stageGoalDTO) {
        if (stageGoalDTO == null || stageGoalDTO.getTitle() == null || stageGoalDTO.getTitle().isEmpty()
                || stageGoalDTO.getCategory() == null || stageGoalDTO.getCategory().isEmpty()
                || stageGoalDTO.getDescription() == null || stageGoalDTO.getDescription().isEmpty()) {
            throw new ParamIllegalException("参数不能为空");
        }
        Long stageGoalId = MyUtils.getPrimaryKey();
        StageGoalPO stageGoalPO = DTOConvertToPO.stageGoalDTOConvertToStageGoalPO(stageGoalDTO);
        stageGoalPO.setId(stageGoalId);
        stageGoalPO.setUserId(10000L);
        goalMapper.insertStageGoal(stageGoalPO);

        if (stageGoalDTO.getSubGoals() != null) {
            List<SubGoalDTO> subGoals = stageGoalDTO.getSubGoals();
            if (subGoals.stream().anyMatch(subGoalDTO ->
                    (subGoalDTO.getTitle() == null || subGoalDTO.getTitle().trim().isEmpty())
                            || subGoalDTO.getContent() == null || subGoalDTO.getContent().trim().isEmpty()
                            || subGoalDTO.getEstimatedHours() == null || subGoalDTO.getEstimatedHours().compareTo(BigDecimal.ZERO) <= 0
            )) {
                throw new ParamIllegalException("小目标标题、内容和估计小时数不能为空");
            }
            List<SubGoalPO> subGoalPOS = new ArrayList<>();
            for (SubGoalDTO subGoal : subGoals) {
                SubGoalPO subGoalPO = DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoal);
                subGoalPO.setId(MyUtils.getPrimaryKey());
                subGoalPO.setStageId(stageGoalId);
                subGoalPO.setUserId(10000L);
                subGoalPOS.add(subGoalPO);
            }
            goalMapper.batchInsertSubGoal(subGoalPOS);
        }
        return ApiResponse.success("新增目标成功");
    }

    @Override
    public ApiResponse<String> batchAddSubGoal(List<SubGoalDTO> subGoalDTOList) {
        List<SubGoalPO> subGoalPOS = new ArrayList<>();
        for (SubGoalDTO subGoalDTO : subGoalDTOList) {
            SubGoalPO subGoalPO = DTOConvertToPO.subGoalDTOConvertToSubGoalPO(subGoalDTO);
            subGoalPO.setId(MyUtils.getPrimaryKey());
            subGoalPO.setUserId(10000L);
            subGoalPOS.add(subGoalPO);
        }
        goalMapper.batchInsertSubGoal(subGoalPOS);
        return ApiResponse.success("批量新增小目标成功");
    }
}
