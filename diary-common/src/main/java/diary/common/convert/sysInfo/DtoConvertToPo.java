package diary.common.convert.sysInfo;

import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.entity.sysInfo.po.IngredientPo;

public final class DtoConvertToPo {

    public static IngredientPo convertToIngredientPo(IngredientReqDto dto) {
        return IngredientPo.builder()
                .category(dto.getCategory())
                .isMain(dto.getIsMain())
                .build();
    }
}
