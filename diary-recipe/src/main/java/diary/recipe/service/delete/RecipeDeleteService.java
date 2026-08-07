package diary.recipe.service.delete;

import diary.common.entity.recipe.dto.req.RecipeCategoryDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeDeleteService {
    ApiResponse<String> deleteRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> deleteCategory(RecipeCategoryDto recipeCategoryDto);
}
