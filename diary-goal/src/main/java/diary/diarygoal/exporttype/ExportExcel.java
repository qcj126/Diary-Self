package diary.diarygoal.exporttype;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.diarygoal.impl.export.ExportServiceImpl;
import diary.diarygoal.service.export.ExportService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExportExcel implements ExportService {
    @Override
    public String export(ExportServiceImpl exportServiceImpl, StageGoalDTO stageGoalDTO) {
        // TODO 实现导出为 Excel 的逻辑
        log.info("导出为 Excel 的逻辑，数据: {}", stageGoalDTO);
        return "成功导出excel数据";
    }
}
