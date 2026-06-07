package diary.common.entity.timemachine.po;

import lombok.Data;

import java.util.Date;

@Data
public class TimeCardPO {
    private Long id;
    private String cardName;
    private String cardTitle;
    private String description;
    private Long categoryId;
    private Integer categoryNum;
    private Integer status;
    private Integer sort;
    private Date createTime;
    private Date updateTime;
    private String updateBy;
    private Long userId;
    private Long imageId;
}
