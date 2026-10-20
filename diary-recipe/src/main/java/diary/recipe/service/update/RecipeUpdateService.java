package diary.recipe.service.update;

import diary.recipe.entity.dto.req.RecipeCategoryDto;
import diary.recipe.entity.dto.req.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeUpdateService {
    ApiResponse<String> updateRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> updateCategory(RecipeCategoryDto recipeCategoryDto);
}
