package diary.diarygoal.service.export;

import diary.common.entity.goal.dto.StageGoalDTO;

public interface ExportService {

    void export(Integer exportType, Integer lastDays, Integer exportSize);
}
