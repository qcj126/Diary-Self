package diary.recipe.service.update;

import diary.common.entity.recipe.dto.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeUpdateService {
    ApiResponse<String> updateRecipe(RecipeReqDto recipeReqDto);
}
