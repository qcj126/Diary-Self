package diary.common.entity.timemachine.po;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class TimeCardPO {
    private Long id;                        // 主键
    private Long userId;                    // 用户ID
    private Long imageId;                   // 图片ID
    private Long categoryId;                // 分类ID
    private Boolean deleted;                // 是否删除  0：在用 1：删除 默认：0
    private Integer cardSort;               // 分类排序
    private String cardTitle;               // 卡片标题
    private String cardContent;             // 卡片内容
    private Date recordTime;                // 记录此事的时间
    private LocalDateTime createdTime;      // 上传时间
    private LocalDateTime updatedTime;      // 更新时间
}
