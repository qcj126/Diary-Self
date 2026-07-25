package diary.common.convert.goal;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;

import java.math.BigDecimal;

public class DTOConvertToPO {
    public static StageGoalPO stageGoalDTOConvertToStageGoalPO(StageGoalDTO stageGoalDTO) {
        return StageGoalPO.builder()
                .id(stageGoalDTO.getId())
                .userId(stageGoalDTO.getUserId())
                .category(stageGoalDTO.getCategory())
                .title(stageGoalDTO.getTitle())
                .description(stageGoalDTO.getDescription() == null ? "" : stageGoalDTO.getDescription())
                .build();
    }

    public static SubGoalPO subGoalDTOConvertToSubGoalPO(SubGoalDTO subGoalDTO) {
        return SubGoalPO.builder()
                .id(subGoalDTO.getId())
                .stageId(subGoalDTO.getStageId())
                .userId(subGoalDTO.getUserId())
                .title(subGoalDTO.getTitle())
                .content(subGoalDTO.getContent() == null ? "" : subGoalDTO.getContent())
                .learnedHours(defaultZero(subGoalDTO.getLearnedHours()))
                .estimatedHours(defaultZero(subGoalDTO.getEstimatedHours()))
                .build();
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
