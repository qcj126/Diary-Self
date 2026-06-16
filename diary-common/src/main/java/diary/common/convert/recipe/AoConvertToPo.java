package diary.common.convert.recipe;

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

        po.setId(keyId);
        po.setRecipeId(recipeId);
        po.setName(ao.getName());
        po.setQuantity(ao.getQuantity());
        po.setIsMain(ao.getIsMain());
        return po;
    }

    public static RecipeStepPO convertToPO(RecipeStepAO ao, Long recipeId, Long keyId) {
        RecipeStepPO po = new RecipeStepPO();
        po.setId(keyId);
        po.setRecipeId(recipeId);
        po.setStepNumber(ao.getStepNumber());
        po.setDescription(ao.getDescription());
        po.setImageUrl(ao.getImageUrl());
        return po;
    }
}
