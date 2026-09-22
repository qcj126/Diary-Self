package diary.diarygoal.service.export;

public interface ExportService {
    GoalExportFile export(Long userId, Integer exportType, Integer ddl, Integer category,
                          Integer lastDays, Integer exportSize);
}
