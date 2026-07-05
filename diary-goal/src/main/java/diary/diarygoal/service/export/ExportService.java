package diary.diarygoal.service.export;

public interface ExportService {
    void export(Integer exportType, Integer lastDays, Integer exportSize);
}
