package diary.common.entity.recipe.dto;

import lombok.Data;

@Data
public class PageReqDto {
    private Integer pageNum = 1;

    private Integer pageSize = 20;

    private String orderBy;
}
