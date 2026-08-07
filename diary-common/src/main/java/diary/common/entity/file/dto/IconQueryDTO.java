package diary.common.entity.file.dto;

import lombok.Data;

@Data
public class IconQueryDTO {
    private Long id;
    private String iconName;
    private Integer iconType;
    private Long userId;
}
