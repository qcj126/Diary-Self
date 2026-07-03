package diary.diarygoal.strategy.service;

import diary.common.entity.goal.dto.StageGoalDTO;

import java.io.ByteArrayOutputStream;

public interface Exporter {
    ByteArrayOutputStream export(StageGoalDTO  stageGoalDTO);

    Integer getCode();
}
