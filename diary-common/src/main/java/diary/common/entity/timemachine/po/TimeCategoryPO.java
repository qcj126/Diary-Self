package diary.common.entity.timemachine.po;

import lombok.Data;

import java.util.Date;

/**
 * 时光机分类PO
 */

@Data
public class TimeCategoryPO {
    private Long id;                // 主键id
    private Long userId;            // 用户id
    private String categoryName;    // 分类名称
    private Integer categoryNum;    // 分类编号
    private Integer status;         // 分类状态  1：在用 2：删除 默认：1
    private Date createTime;        // 创建时间
    private Integer sort;           // 分类排序
    private Date updateTime;        // 更新时间
    private Long updateBy;          // 更新人
}
