package diary.common.convert;

import diary.common.entity.recipe.ao.RecipeIngredientAO;
import diary.common.entity.recipe.ao.RecipeStepAO;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipeStepPO;

public class AoConvertToPo {
    /**
     * 进行AO到PO的转换
     */
    public static RecipeIngredientPO convertToPO(RecipeIngredientAO ao, Long recipeId, Long keyId) {
        RecipeIngredientPO po = new RecipeIngredientPO();
        po.setIngredientId(keyId);
        po.setRecipeId(recipeId);
        po.setName(ao.getName());
        po.setQuantity(ao.getQuantity());
        po.setIsMain(ao.getIsMain());
        po.setSortOrder(ao.getSortOrder());
        return po;
    }

    public static RecipeStepPO convertToPO(RecipeStepAO ao, Long recipeId, Long keyId) {
        RecipeStepPO po = new RecipeStepPO();
        po.setStepId(keyId);
        po.setRecipeId(recipeId);
        po.setStepNumber(ao.getStepNumber());
        po.setDescription(ao.getDescription());
        po.setImageUrl(ao.getImageUrl());
        po.setTimerMin(ao.getTimerMin());
        po.setSortOrder(ao.getSortOrder());
        return po;
    }
}
