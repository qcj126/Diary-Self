package diary.common.convert.recipe;

import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipePO;
import diary.common.entity.recipe.po.RecipeStepPO;
import diary.common.entity.recipe.vo.IngredientVO;
import diary.common.entity.recipe.vo.RecipeVO;
import diary.common.entity.recipe.vo.StepVO;

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
        vo.setImageId(po.getImageId());
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

    public static IngredientVO convertToIngredientVO(RecipeIngredientPO po) {
        if (po == null) {
            return null;
        }
        IngredientVO vo = new IngredientVO();
        vo.setIngredientId(po.getId());
        vo.setName(po.getName());
        vo.setQuantity(po.getQuantity());
        vo.setIsMain(po.getIsMain());
        return vo;
    }

    public static StepVO convertToStepVO(RecipeStepPO po) {
        if (po == null) {
            return null;
        }
        StepVO vo = new StepVO();
        vo.setStepId(po.getId());
        vo.setStepNumber(po.getStepNumber());
        vo.setDescription(po.getDescription());
        vo.setTimerMin(po.getTimerMinute());
        return vo;
    }
}
