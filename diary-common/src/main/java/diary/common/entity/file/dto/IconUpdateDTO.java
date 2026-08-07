package diary.common.entity.file.dto;

import lombok.Data;

@Data
public class IconUpdateDTO {
    private Long id;
    private String iconName;
    private Integer iconType;
    private Integer iconPixel;
    private Long userId;
}
