package diary.common.entity.file.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IconDTO {
    private Long id;
    private String iconName;
    private Integer iconType;
    private String iconPath;
    private Integer iconSize;
    private Integer iconPixel;
    private Long userId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
