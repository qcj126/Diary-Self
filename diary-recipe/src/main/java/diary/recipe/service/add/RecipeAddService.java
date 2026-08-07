package diary.recipe.service.add;


import diary.common.entity.recipe.dto.req.RecipeCategoryDto;
import diary.common.entity.recipe.dto.req.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeAddService {
    ApiResponse<String> addRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> addCategory(RecipeCategoryDto recipeCategoryDto);
}
