package diary.common.convert.goal;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DTOConvertToPO {
    public static StageGoalPO stageGoalDTOConvertToStageGoalPO(StageGoalDTO stageGoalDTO) {
        LocalDateTime now = LocalDateTime.now();
        return StageGoalPO.builder()
                .id(stageGoalDTO.getId())
                .userId(stageGoalDTO.getUserId())
                .creator(stageGoalDTO.getCreator() == null ? String.valueOf(stageGoalDTO.getUserId()) : stageGoalDTO.getCreator())
                .category(stageGoalDTO.getCategory())
                .title(stageGoalDTO.getTitle())
                .description(stageGoalDTO.getDescription() == null ? "" : stageGoalDTO.getDescription())
                .deleted(false)
                .createTime(now)
                .updateTime(now)
                .build();
    }

    public static SubGoalPO subGoalDTOConvertToSubGoalPO(SubGoalDTO subGoalDTO) {
        LocalDateTime now = LocalDateTime.now();
        return SubGoalPO.builder()
                .id(subGoalDTO.getId())
                .stageGoalId(subGoalDTO.getStageGoalId())
                .userId(subGoalDTO.getUserId())
                .title(subGoalDTO.getTitle())
                .content(subGoalDTO.getContent() == null ? "" : subGoalDTO.getContent())
                .learnedHours(defaultZero(subGoalDTO.getLearnedHours()))
                .estimatedHours(defaultZero(subGoalDTO.getEstimatedHours()))
                .deleted(false)
                .createTime(now)
                .updateTime(now)
                .build();
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
