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
    private String cardTitle;               // 卡片标题
    private String cardContent;             // 卡片内容
    private Date recordTime;                // 记录此事的时间
    private Integer deleted;                // 是否删除：0-否 1-是
    private LocalDateTime createTime;       // 创建时间
    private LocalDateTime updateTime;       // 更新时间
}
