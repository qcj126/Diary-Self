package diary.diarygoal.impl.export;

import diary.common.entity.goal.dto.GoalQueryDTO;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.diarygoal.factory.ExporterFactory;
import diary.diarygoal.mapper.GoalMapper;
import diary.diarygoal.service.export.ExportService;
import diary.diarygoal.service.export.GoalExportFile;
import diary.diarygoal.strategy.service.Exporter;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportServiceImpl implements ExportService {
    @Resource
    private GoalMapper goalMapper;

    @Resource
    private ExporterFactory exporterFactory;

    @Override
    public GoalExportFile export(Long userId, Integer exportType, Integer ddl, Integer category,
                                 Integer lastDays, Integer exportSize) {
        GoalQueryDTO query = new GoalQueryDTO();
        query.setUserId(userId);
        query.setCategory(resolveCategory(category));
        query.setDdl(ddl);
        query.setRecentDays(lastDays == null || lastDays == 0 ? null : lastDays);
        query.setExportSize(exportSize);

        List<StageGoalPO> stageGoalPOList = goalMapper.selectStageGoals(query);
        if (stageGoalPOList == null || stageGoalPOList.isEmpty()) {
            throw new IllegalArgumentException("没有符合条件的阶段目标可导出");
        }

        Exporter exporter = exporterFactory.getExporter(exportType);
        List<Long> stageIds = stageGoalPOList.stream().map(StageGoalPO::getId).toList();
        List<SubGoalPO> subGoals = goalMapper.selectSubGoalsByStageIds(stageIds);
        Map<Long, List<SubGoalPO>> subGoalMap = (subGoals == null ? List.<SubGoalPO>of() : subGoals).stream()
                .collect(java.util.stream.Collectors.groupingBy(SubGoalPO::getStageId));
        List<StageGoalDTO> goals = stageGoalPOList.stream()
                .map(goal -> convertToDTO(goal, subGoalMap.getOrDefault(goal.getId(), List.of())))
                .toList();

        return goals.size() == 1
                ? exportSingle(exporter, goals.getFirst(), exportType)
                : exportZip(exporter, goals, exportType);
    }

    private GoalExportFile exportSingle(Exporter exporter, StageGoalDTO goal, Integer exportType) {
        byte[] content = exporter.export(goal).toByteArray();
        String extension = extensionOf(exportType);
        return new GoalExportFile(content, "stage-goal-export." + extension, contentTypeOf(exportType));
    }

    private GoalExportFile exportZip(Exporter exporter, List<StageGoalDTO> goals, Integer exportType) {
        String extension = extensionOf(exportType);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
            for (int index = 0; index < goals.size(); index++) {
                zip.putNextEntry(new ZipEntry("stage-goal-" + (index + 1) + "." + extension));
                zip.write(exporter.export(goals.get(index)).toByteArray());
                zip.closeEntry();
            }
            zip.finish();
            return new GoalExportFile(output.toByteArray(), "stage-goals-export.zip", "application/zip");
        } catch (IOException e) {
            throw new IllegalStateException("打包导出文件失败", e);
        }
    }

    private StageGoalDTO convertToDTO(StageGoalPO stageGoalPO, List<SubGoalPO> subGoalPOList) {
        StageGoalDTO dto = new StageGoalDTO();
        dto.setId(stageGoalPO.getId());
        dto.setUserId(stageGoalPO.getUserId());
        dto.setCreator(stageGoalPO.getCreator());
        dto.setCategory(stageGoalPO.getCategory());
        dto.setTitle(stageGoalPO.getTitle());
        dto.setDescription(stageGoalPO.getDescription());
        dto.setDdl(stageGoalPO.getEndTime());
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
        dto.setDdl(subGoalPO.getEndTime());
        return dto;
    }

    private String resolveCategory(Integer category) {
        return switch (category) {
            case 1 -> null;
            case 2 -> "技术";
            case 3 -> "学习";
            case 4 -> "健康";
            case 5 -> "生活";
            default -> throw new IllegalArgumentException("category 只能是 1 到 5");
        };
    }

    private String extensionOf(Integer exportType) {
        return switch (exportType) {
            case 1 -> "xlsx";
            case 2 -> "pdf";
            case 3 -> "png";
            default -> throw new IllegalArgumentException("exportType 只能是 1、2 或 3");
        };
    }

    private String contentTypeOf(Integer exportType) {
        return switch (exportType) {
            case 1 -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case 2 -> "application/pdf";
            case 3 -> "image/png";
            default -> throw new IllegalArgumentException("exportType 只能是 1、2 或 3");
        };
    }
}
