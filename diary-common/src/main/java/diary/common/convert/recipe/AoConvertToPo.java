package diary.common.convert.recipe;

import diary.common.entity.recipe.ao.RecipeIngredientAO;
import diary.common.entity.recipe.ao.RecipeStepAO;
import diary.common.entity.recipe.po.RecipeIngredientPO;
import diary.common.entity.recipe.po.RecipeStepPO;

import java.time.LocalDateTime;

public class AoConvertToPo {
    /**
     * 进行AO到PO的转换
     */
    public static RecipeIngredientPO convertToPO(RecipeIngredientAO ao, Long recipeId, Long keyId) {
        return convertToPO(ao, recipeId, null, keyId);
    }

    public static RecipeIngredientPO convertToPO(RecipeIngredientAO ao, Long recipeId, Long userId, Long keyId) {
        RecipeIngredientPO po = new RecipeIngredientPO();
        LocalDateTime now = LocalDateTime.now();

        po.setId(keyId);
        po.setRecipeId(recipeId);
        po.setUserId(userId);
        po.setName(ao.getName());
        po.setQuantity(ao.getQuantity());
        po.setIsMain(ao.getIsMain());
        po.setSort(ao.getSort() == null ? 0 : ao.getSort());
        po.setDeleted(0);
        po.setCreateTime(now);
        po.setUpdateTime(now);
        return po;
    }

    public static RecipeStepPO convertToPO(RecipeStepAO ao, Long recipeId, Long keyId) {
        return convertToPO(ao, recipeId, null, keyId);
    }

    public static RecipeStepPO convertToPO(RecipeStepAO ao, Long recipeId, Long userId, Long keyId) {
        RecipeStepPO po = new RecipeStepPO();
        LocalDateTime now = LocalDateTime.now();

        po.setId(keyId);
        po.setRecipeId(recipeId);
        po.setUserId(userId);
        po.setStepNumber(ao.getStepNumber());
        po.setDescription(ao.getDescription());
        po.setTimerMinute(ao.getTimerMin() == null ? 0 : ao.getTimerMin());
        po.setSort(ao.getSort() == null ? po.getStepNumber() : ao.getSort());
        po.setDeleted(0);
        po.setCreateTime(now);
        po.setUpdateTime(now);
        return po;
    }
}
