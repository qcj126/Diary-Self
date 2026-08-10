package diary.common.convert.recipe;

import diary.common.entity.recipe.dto.db.RecipeCategoryIconDto;
import diary.common.entity.recipe.vo.RecipeCategoryVO;
import lombok.Data;

@Data
public class DtoConvertToVo {
    public static RecipeCategoryVO convertToRecipeCategoryVO(RecipeCategoryIconDto dto) {
        return RecipeCategoryVO.builder()
                .id(dto.getCategoryId())
                .userId(dto.getUserId())
                .categoryName(dto.getCategoryName())
                .categoryNum(dto.getCategoryNum())
                .iconId(dto.getIconId())
                .iconPath(dto.getIconPath())
                .sort(dto.getSort())
                .build();
    }
}
