package diary.recipe.service.update;

import diary.common.entity.recipe.dto.RecipeReqDto;
import diary.common.result.ApiResponse;

import java.util.Map;

public interface RecipeUpdateService {
    ApiResponse<Map<String, Object>> updateRecipe(RecipeReqDto recipeReqDto);
}
