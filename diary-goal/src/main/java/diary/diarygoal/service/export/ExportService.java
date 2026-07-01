package diary.diarygoal.service.export;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.diarygoal.impl.export.ExportServiceImpl;

public interface ExportService {
    String export(ExportServiceImpl exportServiceImpl, StageGoalDTO stageGoalDTO);
}
