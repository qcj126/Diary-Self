package diary.recipe.service.update;

import diary.common.entity.recipe.dto.req.RecipeCategoryDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeUpdateService {
    ApiResponse<String> updateRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> updateCategory(RecipeCategoryDto recipeCategoryDto);
}
