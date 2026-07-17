package diary.common.convert.recipe;

import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.vo.RecipeVO;

public class PoConvertToVo {

    /**
     * RecipePO 转 RecipeVO
     */
    public static RecipeVO convertToRecipeVO(RecipePO po) {
        if (po == null) {
            return null;
        }
        RecipeVO vo = new RecipeVO();
        vo.setRecipeId(po.getId());
        vo.setTitle(po.getTitle());
        vo.setCoverImg(po.getCoverImg());
        vo.setDescription(po.getDescription());
        vo.setCategory(po.getCategory());
        vo.setMealType(po.getMealType());
        vo.setDifficulty(po.getDifficulty());
        vo.setCookingTime(po.getCookingTime());
        vo.setStory(po.getStory());
        vo.setCreatedAt(po.getCreateTime());
        vo.setUpdatedAt(po.getUpdateTime());
        return vo;
    }
}
