package diary.common.entity.file.dto;

import lombok.Data;

@Data
public class IconAddDTO {
    private String iconName;
    private Integer iconType;
    private Integer iconPixel;
    private Long userId;
}
