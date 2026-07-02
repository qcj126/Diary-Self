package diary.diarygoal.impl.export;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.diarygoal.factory.ExporterFactory;
import diary.diarygoal.impl.query.GoalQueryServiceImpl;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.export.ExportService;
import diary.diarygoal.strategy.Exporter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ExportServiceImpl implements ExportService {
    @Resource
    private GoalMapper goalMapper;

    @Resource
    private GoalQueryServiceImpl goalQueryServiceImpl;

    @Resource
    private ExporterFactory exporterFactory;

    @Override
    public void export(Integer exportType, Integer lastDays, Integer exportSize) {
        StageGoalDTO stageGoalDTO = goalMapper.queryGoalData(lastDays, exportSize);
        Exporter exporter = exporterFactory.getExporter(exportType);
        ByteArrayOutputStream exportDataStream = exporter.export(stageGoalDTO);
    }
}
