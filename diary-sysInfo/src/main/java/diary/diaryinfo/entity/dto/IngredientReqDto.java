package diary.diaryinfo.entity.dto;

import lombok.Data;

@Data
public class IngredientReqDto {
    private String category;
    private Integer isMain;
    private String name;
    private String categoryName;
    private Long iconId;
}
