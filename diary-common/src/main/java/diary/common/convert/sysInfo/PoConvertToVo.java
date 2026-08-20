package diary.common.convert.sysInfo;

import diary.common.entity.sysInfo.po.CookWayPo;
import diary.common.entity.sysInfo.po.IngredientPo;
import diary.common.entity.sysInfo.vo.CookWayVo;
import diary.common.entity.sysInfo.vo.IngredientCategoryVo;
import diary.common.entity.sysInfo.vo.IngredientVo;

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
