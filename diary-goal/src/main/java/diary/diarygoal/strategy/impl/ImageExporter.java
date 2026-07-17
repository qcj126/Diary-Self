package diary.diarygoal.strategy.impl;

import diary.common.entity.goal.dto.StageGoalDTO;
import diary.common.entity.goal.dto.SubGoalDTO;
import diary.common.enums.exportenum.ExportEnum;
import diary.diarygoal.strategy.service.Exporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Image导出器 - 使用Java2D将阶段目标数据渲染为PNG图片
 */
@Slf4j
@Component
@Order(1)
public class ImageExporter implements Exporter {

    /** 图片样式常量 */
    private static final int IMG_WIDTH = 800;
    private static final int PADDING = 30;
    private static final int LINE_HEIGHT = 28;
    private static final int HEADER_ROW_HEIGHT = 32;
    private static final int DATA_ROW_HEIGHT = 26;
    private static final Color BG_COLOR = new Color(255, 255, 255);
    private static final Color HEADER_BG = new Color(70, 130, 180);
    private static final Color ALT_ROW_BG = new Color(240, 248, 255);
    private static final Color TEXT_COLOR = new Color(33, 33, 33);
    private static final Color BORDER_COLOR = new Color(200, 200, 200);

    /** 表头列定义 */
    private static final String[] HEADERS = {"序号", "子目标", "内容", "已学时长(h)", "预估时长(h)"};
    /** 列宽比例 */
    private static final float[] COL_RATIOS = {0.08f, 0.22f, 0.35f, 0.175f, 0.175f};

    @Override
    public ByteArrayOutputStream export(StageGoalDTO stageGoalDTO) {
        try {
            List<SubGoalDTO> subGoals = stageGoalDTO.getSubGoals();
            int subCount = subGoals != null ? subGoals.size() : 0;

            // 计算图片高度：标题区 + 信息区 + 表格区
            int infoHeight = LINE_HEIGHT * 4 + 10;
            int tableHeight = HEADER_ROW_HEIGHT + subCount * DATA_ROW_HEIGHT + 20;
            int totalHeight = PADDING + LINE_HEIGHT + 20 + infoHeight + tableHeight + PADDING;

            BufferedImage image = new BufferedImage(IMG_WIDTH, totalHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // 开启抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 白色背景
            g.setColor(BG_COLOR);
            g.fillRect(0, 0, IMG_WIDTH, totalHeight);

            int y = PADDING;
            Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 20);
            Font infoFont = new Font("Microsoft YaHei", Font.PLAIN, 14);
            Font tableFont = new Font("Microsoft YaHei", Font.PLAIN, 12);
            Font headerFont = new Font("Microsoft YaHei", Font.BOLD, 13);

            // 绘制标题
            g.setFont(titleFont);
            g.setColor(TEXT_COLOR);
            FontMetrics fm = g.getFontMetrics();
            String title = nullSafe(stageGoalDTO.getTitle());
            g.drawString(title, (IMG_WIDTH - fm.stringWidth(title)) / 2, y + fm.getAscent());
            y += LINE_HEIGHT + 20;

            // 绘制基本信息
            g.setFont(infoFont);
            g.setColor(new Color(100, 100, 100));
            y = drawInfoLine(g, y, "分类: " + nullSafe(stageGoalDTO.getCategory()));
            y = drawInfoLine(g, y, "创建者: " + nullSafe(stageGoalDTO.getCreator()));
            y = drawInfoLine(g, y, "描述: " + nullSafe(stageGoalDTO.getDescription()));
            y += 10;

            // 绘制子目标表格
            drawTable(g, y, subGoals, tableFont, headerFont);

            g.dispose();

            // 输出为PNG
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", os);
            return os;
        } catch (IOException e) {
            log.error("导出Image失败", e);
            throw new RuntimeException("导出Image失败", e);
        }
    }

    /** 绘制一行信息文本 */
    private int drawInfoLine(Graphics2D g, int y, String text) {
        g.drawString(text, PADDING, y + g.getFontMetrics().getAscent());
        return y + LINE_HEIGHT;
    }

    /** 绘制子目标表格 */
    private void drawTable(Graphics2D g, int startY, List<SubGoalDTO> subGoals, Font tableFont, Font headerFont) {
        int tableX = PADDING;
        int tableWidth = IMG_WIDTH - 2 * PADDING;

        // 计算各列宽度
        int[] colWidths = new int[COL_RATIOS.length];
        for (int i = 0; i < COL_RATIOS.length; i++) {
            colWidths[i] = Math.round(tableWidth * COL_RATIOS[i]);
        }

        // 绘制表头
        g.setFont(headerFont);
        g.setColor(HEADER_BG);
        g.fillRect(tableX, startY, tableWidth, HEADER_ROW_HEIGHT);
        g.setColor(Color.WHITE);
        int x = tableX;
        for (int i = 0; i < HEADERS.length; i++) {
            drawCenteredString(g, HEADERS[i], x, startY, colWidths[i], HEADER_ROW_HEIGHT);
            x += colWidths[i];
        }

        // 绘制数据行
        g.setFont(tableFont);
        if (subGoals != null && !subGoals.isEmpty()) {
            int y = startY + HEADER_ROW_HEIGHT;
            int seq = 1;
            for (SubGoalDTO sub : subGoals) {
                // 交替行背景色
                if (seq % 2 == 0) {
                    g.setColor(ALT_ROW_BG);
                    g.fillRect(tableX, y, tableWidth, DATA_ROW_HEIGHT);
                }
                g.setColor(TEXT_COLOR);

                String[] rowData = {
                        String.valueOf(seq),
                        nullSafe(sub.getTitle()),
                        nullSafe(sub.getContent()),
                        formatHours(sub.getLearnedHours()),
                        formatHours(sub.getEstimatedHours())
                };
                x = tableX;
                for (int i = 0; i < rowData.length; i++) {
                    drawCenteredString(g, rowData[i], x, y, colWidths[i], DATA_ROW_HEIGHT);
                    x += colWidths[i];
                }
                y += DATA_ROW_HEIGHT;
                seq++;
            }
        }

        // 绘制表格边框
        g.setColor(BORDER_COLOR);
        int tableHeight = HEADER_ROW_HEIGHT + (subGoals != null ? subGoals.size() : 0) * DATA_ROW_HEIGHT;
        g.drawRect(tableX, startY, tableWidth, tableHeight);
        // 绘制列分隔线
        int lx = tableX;
        for (int i = 0; i < colWidths.length - 1; i++) {
            lx += colWidths[i];
            g.drawLine(lx, startY, lx, startY + tableHeight);
        }
    }

    /** 在指定区域内居中绘制字符串 */
    private void drawCenteredString(Graphics2D g, String text, int x, int y, int cellWidth, int cellHeight) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int drawX = x + (cellWidth - textWidth) / 2;
        int drawY = y + (cellHeight + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, drawX, drawY);
    }

    private String formatHours(java.math.BigDecimal hours) {
        return hours != null ? hours.toPlainString() : "0";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    @Override
    public Integer getCode() {
        return ExportEnum.IMAGE.getCode();
    }
}
