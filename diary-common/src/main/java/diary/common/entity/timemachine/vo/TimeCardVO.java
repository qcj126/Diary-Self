package diary.common.entity.timemachine.vo;

import lombok.Data;

import java.util.Date;

@Data
public class TimeCardVO {
    private Long id;
    private Long userId;
    private Long imageId;
    private Long categoryId;
    private String cardTitle;
    private String cardContent;
    private Date recordTime;
    private Integer deleted;
    private Date createTime;
    private Date updateTime;
}
