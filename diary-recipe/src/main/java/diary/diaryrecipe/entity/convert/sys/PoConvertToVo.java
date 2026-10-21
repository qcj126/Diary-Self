package diary.diaryrecipe.entity.convert.sys;

import diary.diaryrecipe.entity.po.CookWayPo;
import diary.diaryrecipe.entity.po.IngredientPo;
import diary.diaryrecipe.entity.vo.CookWayVo;
import diary.diaryrecipe.entity.vo.IngredientCategoryVo;
import diary.diaryrecipe.entity.vo.sys.IngredientVo;

public final class PoConvertToVo {
    private PoConvertToVo() {
    }

    public static IngredientCategoryVo convertToIngredientCategoryVo(IngredientPo po) {
        if (po == null) {
            return null;
        }
        IngredientCategoryVo vo = new IngredientCategoryVo();
        vo.setId(po.getId());
        vo.setCategory(po.getCategory());
        vo.setCategoryName(po.getCategoryName());
        return vo;
    }

    public static IngredientVo convertToIngredientVo(IngredientPo po) {
        if (po == null) {
            return null;
        }
        return IngredientVo.builder()
                        .id(po.getId())
                        .name(po.getName())
                        .category(po.getCategory())
                        .categoryName(po.getCategoryName())
                        .isMain(po.getIsMain())
                        .iconId(po.getIconId())
                        .build();
    }

    public static CookWayVo convertToCookWayVo(CookWayPo po) {
        if (po == null) {
            return null;
        }
        CookWayVo vo = new CookWayVo();
        vo.setId(po.getId());
        vo.setName(po.getName());
        vo.setDescription(po.getDescription());
        return vo;
    }
}
