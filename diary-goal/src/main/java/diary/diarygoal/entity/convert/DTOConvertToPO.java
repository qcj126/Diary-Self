package diary.diarygoal.entity.convert;

import diary.diarygoal.entity.dto.StageGoalDTO;
import diary.diarygoal.entity.dto.SubGoalDTO;
import diary.diarygoal.entity.po.StageGoalPO;
import diary.diarygoal.entity.po.SubGoalPO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DTOConvertToPO {
    public static StageGoalPO stageGoalDTOConvertToStageGoalPO(StageGoalDTO stageGoalDTO) {
        return StageGoalPO.builder()
                .id(stageGoalDTO.getId())
                .userId(stageGoalDTO.getUserId())
                .category(stageGoalDTO.getCategory())
                .title(stageGoalDTO.getTitle())
                .description(stageGoalDTO.getDescription() == null ? "" : stageGoalDTO.getDescription())
                .status(stageGoalDTO.getStatus())
                .endTime(resolveEndTime(stageGoalDTO.getEndTime(), stageGoalDTO.getDdl()))
                .build();
    }

    public static SubGoalPO subGoalDTOConvertToSubGoalPO(SubGoalDTO subGoalDTO) {
        return SubGoalPO.builder()
                .id(subGoalDTO.getId())
                .stageId(resolveStageId(subGoalDTO))
                .userId(subGoalDTO.getUserId())
                .title(subGoalDTO.getTitle())
                .content(subGoalDTO.getContent() == null ? "" : subGoalDTO.getContent())
                .learnedHours(defaultZero(subGoalDTO.getLearnedHours()))
                .estimatedHours(defaultZero(subGoalDTO.getEstimatedHours()))
                .endTime(resolveEndTime(subGoalDTO.getEndTime(), subGoalDTO.getDdl()))
                .build();
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Long resolveStageId(SubGoalDTO subGoalDTO) {
        return subGoalDTO.getStageId() == null ? subGoalDTO.getStageGoalId() : subGoalDTO.getStageId();
    }

    private static LocalDateTime resolveEndTime(LocalDateTime endTime, LocalDateTime ddl) {
        if (endTime != null) {
            return endTime;
        }
        return ddl;
    }
}
