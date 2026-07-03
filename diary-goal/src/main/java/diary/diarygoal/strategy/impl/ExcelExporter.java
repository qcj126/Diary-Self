package diary.diarygoal.strategy.impl;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.enums.exportenum.ExportEnum;
import diary.diarygoal.strategy.service.Exporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Slf4j
@Component
public class ExcelExporter implements Exporter {
    @Override
    public ByteArrayOutputStream export(StageGoalDTO stageGoalDTO) {
        // TODO 实现导出为 Excel 的逻辑
        log.info("导出为 Excel 的逻辑，数据: {}", stageGoalDTO);
        return null;
    }

    @Override
    public Integer getCode() {
        return ExportEnum.EXCEL.getCode();
    }
}
