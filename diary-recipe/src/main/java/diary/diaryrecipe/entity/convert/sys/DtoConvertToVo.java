package diary.diaryrecipe.entity.convert.sys;

import diary.diaryrecipe.entity.dto.IngredientIconDto;
import diary.diaryrecipe.entity.vo.sys.IngredientVo;

public class DtoConvertToVo {
    public static IngredientVo convertToVo(IngredientIconDto ingredientIconDto) {

        return IngredientVo.builder()
                .id(ingredientIconDto.getId())
                .name(ingredientIconDto.getName())
                .category(ingredientIconDto.getCategory())
                .categoryName(ingredientIconDto.getCategoryName())
                .isMain(ingredientIconDto.getIsMain())
                .iconId(ingredientIconDto.getIconId())
                .iconName(ingredientIconDto.getIconName())
                .iconPath(ingredientIconDto.getIconPath())
                .userId(ingredientIconDto.getUserId())
                .build();
    }
}
