package diary.diarygoal.strategy.service;

import diary.diarygoal.entity.dto.StageGoalDTO;

import java.io.ByteArrayOutputStream;

public interface Exporter {
    ByteArrayOutputStream export(StageGoalDTO  stageGoalDTO);

    Integer getCode();
}
