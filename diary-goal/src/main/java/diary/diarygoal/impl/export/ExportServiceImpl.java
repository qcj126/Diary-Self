package diary.diarygoal.impl.export;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.exception.ParamIllegalException;
import diary.diarygoal.factory.ExporterFactory;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.export.ExportService;
import diary.diarygoal.strategy.service.Exporter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {
    @Resource
    private GoalMapper goalMapper;

    @Resource
    private ExporterFactory exporterFactory;

    @Override
    public void export(Integer exportType, Integer lastDays, Integer exportSize) {
        List<StageGoalPO> stageGoalPOList = goalMapper.queryGoalData(lastDays, exportSize);
        if (stageGoalPOList == null || stageGoalPOList.isEmpty()) {
            throw new ParamIllegalException("no goal data to export");
        }
        StageGoalPO stageGoalPO = stageGoalPOList.get(0);
        StageGoalDTO stageGoalDTO = convertToDTO(stageGoalPO, goalMapper.selectSubGoalsByStageId(stageGoalPO.getId()));
        Exporter exporter = exporterFactory.getExporter(exportType);
        ByteArrayOutputStream exportDataStream = exporter.export(stageGoalDTO);
    }

    private StageGoalDTO convertToDTO(StageGoalPO stageGoalPO, List<SubGoalPO> subGoalPOList) {
        StageGoalDTO dto = new StageGoalDTO();
        dto.setId(stageGoalPO.getId());
        dto.setUserId(stageGoalPO.getUserId());
        dto.setCategory(stageGoalPO.getCategory());
        dto.setTitle(stageGoalPO.getTitle());
        dto.setDescription(stageGoalPO.getDescription());
        dto.setSubGoals(subGoalPOList == null ? List.of() : subGoalPOList.stream().map(this::convertToDTO).toList());
        return dto;
    }

    private SubGoalDTO convertToDTO(SubGoalPO subGoalPO) {
        SubGoalDTO dto = new SubGoalDTO();
        dto.setId(subGoalPO.getId());
        dto.setStageId(subGoalPO.getStageId());
        dto.setUserId(subGoalPO.getUserId());
        dto.setTitle(subGoalPO.getTitle());
        dto.setContent(subGoalPO.getContent());
        dto.setLearnedHours(subGoalPO.getLearnedHours());
        dto.setEstimatedHours(subGoalPO.getEstimatedHours());
        return dto;
    }
}
