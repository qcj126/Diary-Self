package diary.common.entity.diet.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DietRecordImagePO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 饮食记录ID
     */
    private Long recordId;

    /**
     * 图片表ID
     */
    private Long imageId;

    /**
     * 图片展示顺序，越小越靠前
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}