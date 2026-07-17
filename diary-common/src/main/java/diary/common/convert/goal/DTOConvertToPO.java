package diary.common.convert.goal;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;

public class DTOConvertToPO {
    public static StageGoalPO stageGoalDTOConvertToStageGoalPO(StageGoalDTO stageGoalDTO) {
        return StageGoalPO.builder()
                        .id(stageGoalDTO.getId())
                        .userId(stageGoalDTO.getUserId())
                        .creator(stageGoalDTO.getCreator() == null ? String.valueOf(stageGoalDTO.getUserId()) : stageGoalDTO.getCreator())
                        .category(stageGoalDTO.getCategory())
                        .title(stageGoalDTO.getTitle())
                        .description(stageGoalDTO.getDescription())
                        .deleted(false)
                        .build();
    }

    public static SubGoalPO subGoalDTOConvertToSubGoalPO(SubGoalDTO subGoalDTO) {
        return SubGoalPO.builder()
                .id(subGoalDTO.getId())
                .stageGoalId(subGoalDTO.getStageGoalId())
                .userId(subGoalDTO.getUserId())
                .title(subGoalDTO.getTitle())
                .content(subGoalDTO.getContent())
                .learnedHours(subGoalDTO.getLearnedHours())
                .estimatedHours(subGoalDTO.getEstimatedHours())
                .deleted(false)
                .build();
    }
}
