package diary.diarygoal.strategy.impl;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.enums.exportenum.ExportEnum;
import diary.diarygoal.strategy.service.Exporter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Excel导出器 - 使用Apache POI将阶段目标数据导出为.xlsx文件
 */
@Slf4j
@Component
@Order(1)
public class ExcelExporter implements Exporter {

    /** 表头列定义 */
    private static final String[] HEADERS = {"序号", "子目标", "内容", "已学时长(h)", "预估时长(h)"};

    @Override
    public ByteArrayOutputStream export(StageGoalDTO stageGoalDTO) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream os = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("阶段目标");

            // 写入目标基本信息行
            int rowIdx = writeGoalInfo(sheet, stageGoalDTO);

            // 写入子目标表头和数据行
            writeSubGoals(sheet, rowIdx, stageGoalDTO.getSubGoals());

            // 自动调整列宽
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(os);
            return os;
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * 写入阶段目标基本信息（标题、分类、创建者、描述）
     */
    private int writeGoalInfo(Sheet sheet, StageGoalDTO dto) {
        String[][] info = {
                {"标题", dto.getTitle()},
                {"分类", dto.getCategory()},
                {"创建者", dto.getCreator()},
                {"描述", dto.getDescription()}
        };
        int rowIdx = 0;
        for (String[] pair : info) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(pair[0]);
            row.createCell(1).setCellValue(pair[1] != null ? pair[1] : "");
        }
        // 空行分隔
        rowIdx++;
        return rowIdx;
    }

    /**
     * 写入子目标表头及数据行
     */
    private void writeSubGoals(Sheet sheet, int startRow, List<SubGoalDTO> subGoals) {
        // 表头
        Row headerRow = sheet.createRow(startRow);
        for (int i = 0; i < HEADERS.length; i++) {
            headerRow.createCell(i).setCellValue(HEADERS[i]);
        }

        // 数据行
        if (subGoals == null || subGoals.isEmpty()) {
            return;
        }
        int rowIdx = startRow + 1;
        int seq = 1;
        for (SubGoalDTO sub : subGoals) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(seq++);
            row.createCell(1).setCellValue(nullSafe(sub.getTitle()));
            row.createCell(2).setCellValue(nullSafe(sub.getContent()));
            row.createCell(3).setCellValue(sub.getLearnedHours() != null ? sub.getLearnedHours().doubleValue() : 0);
            row.createCell(4).setCellValue(sub.getEstimatedHours() != null ? sub.getEstimatedHours().doubleValue() : 0);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    @Override
    public Integer getCode() {
        return ExportEnum.EXCEL.getCode();
    }
}
