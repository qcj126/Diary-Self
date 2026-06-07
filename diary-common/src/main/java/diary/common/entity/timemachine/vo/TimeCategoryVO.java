package diary.common.entity.timemachine.vo;

import lombok.Data;

import java.util.Date;

/**
 * 时光机分类PO
 */

@Data
public class TimeCategoryVO {
    private Long id;
    private String userId;
    private String categoryName;
    private Integer categoryNum;
    private Integer status;
    private String createName;
    private Date createTime;
    private Integer sort;
}
