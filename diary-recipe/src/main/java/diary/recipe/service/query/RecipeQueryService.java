package diary.recipe.service.query;

import diary.common.entity.recipe.dto.RecipeReqDto;
import diary.common.result.ApiResponse;

import java.util.Map;

public interface RecipeQueryService {
    ApiResponse<Map<String, Object>> queryRecipe(RecipeReqDto recipeReqDto);
}
