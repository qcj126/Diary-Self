package diary.diarygoal.factory;

import diary.diarygoal.exporttype.ExportExcel;
import diary.diarygoal.exporttype.ExportImage;
import diary.diarygoal.exporttype.ExportPDF;
import diary.diarygoal.service.export.ExportService;
import org.springframework.stereotype.Component;

@Component
public class ExportFactory {
    public static ExportService getExportService(Integer exportType) {
        return switch (exportType) {
            case 1 -> new ExportExcel();
            case 2 -> new ExportPDF();
            case 3 -> new ExportImage();
            default -> throw new IllegalArgumentException("Invalid exportType: " + exportType);
        };
    }
}
