package diary.common.entity.timemachine.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TimeCardVO {
    private Long id;
    private String cardName;
    private String cardTitle;
    private String description;
    private Long categoryId;
    private Integer categoryNum;
    private Integer status;
    private Integer sort;
    private Date createTime;
    private String createBy;
    private Date updateTime;
    private String updateBy;
    private Long userId;
    private Long imageId;
}
