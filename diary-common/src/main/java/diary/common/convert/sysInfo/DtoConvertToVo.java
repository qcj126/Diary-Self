package diary.common.convert.sysInfo;

import diary.common.entity.sysInfo.dto.IngredientIconDto;
import diary.common.entity.sysInfo.vo.IngredientVo;

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
