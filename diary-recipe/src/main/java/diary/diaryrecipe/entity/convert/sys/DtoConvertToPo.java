package diary.diaryrecipe.entity.convert.sys;

import diary.diaryrecipe.entity.dto.IngredientReqDto;
import diary.diaryrecipe.entity.po.IngredientPo;

public final class DtoConvertToPo {

    public static IngredientPo convertToIngredientPo(IngredientReqDto dto) {
        return IngredientPo.builder()
                .category(dto.getCategory())
                .isMain(dto.getIsMain())
                .build();
    }
}
