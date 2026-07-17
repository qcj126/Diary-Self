package diary.diarygoal.strategy.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.enums.exportenum.ExportEnum;
import diary.diarygoal.strategy.service.Exporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * PDF导出器 - 使用OpenPDF将阶段目标数据导出为PDF文件
 */
@Slf4j
@Component
@Order(1)
public class PdfExporter implements Exporter {

    /** 表头列定义 */
    private static final String[] HEADERS = {"序号", "子目标", "内容", "已学时长(h)", "预估时长(h)"};

    @Override
    public ByteArrayOutputStream export(StageGoalDTO stageGoalDTO) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, os);
            document.open();

            // 使用系统默认中文字体
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UTF16-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font infoFont = new Font(bfChinese, 12, Font.NORMAL);
            Font headerFont = new Font(bfChinese, 11, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(bfChinese, 10, Font.NORMAL);

            // 写入标题
            Paragraph title = new Paragraph(nullSafe(stageGoalDTO.getTitle()), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);

            // 写入基本信息
            addInfoLine(document, "分类: " + nullSafe(stageGoalDTO.getCategory()), infoFont);
            addInfoLine(document, "创建者: " + nullSafe(stageGoalDTO.getCreator()), infoFont);
            addInfoLine(document, "描述: " + nullSafe(stageGoalDTO.getDescription()), infoFont);
            document.add(new Paragraph(" ", infoFont)); // 空行

            // 写入子目标表格
            addSubGoalTable(document, stageGoalDTO.getSubGoals(), headerFont, cellFont);

            document.close();
            return os;
        } catch (Exception e) {
            log.error("导出PDF失败", e);
            throw new RuntimeException("导出PDF失败", e);
        }
    }

    /** 添加一行基本信息 */
    private void addInfoLine(Document document, String text, Font font) throws DocumentException {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingAfter(5);
        document.add(p);
    }

    /** 添加子目标表格 */
    private void addSubGoalTable(Document document, List<SubGoalDTO> subGoals, Font headerFont, Font cellFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // 表头行（深色背景 + 白色字体）
        Color headerBg = new Color(70, 130, 180);
        for (String header : HEADERS) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerBg);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        // 数据行
        if (subGoals != null) {
            int seq = 1;
            for (SubGoalDTO sub : subGoals) {
                addCell(table, String.valueOf(seq++), cellFont);
                addCell(table, sub.getTitle(), cellFont);
                addCell(table, sub.getContent(), cellFont);
                addCell(table, formatHours(sub.getLearnedHours()), cellFont);
                addCell(table, formatHours(sub.getEstimatedHours()), cellFont);
            }
        }

        document.add(table);
    }

    /** 添加表格单元格 */
    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(nullSafe(text), font));
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private String formatHours(java.math.BigDecimal hours) {
        return hours != null ? hours.toPlainString() : "0";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    @Override
    public Integer getCode() {
        return ExportEnum.PDF.getCode();
    }
}
