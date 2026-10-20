package diary.recipe.service.add;


import diary.recipe.entity.dto.req.RecipeCategoryDto;
import diary.recipe.entity.dto.req.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeAddService {
    ApiResponse<String> addRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> addCategory(RecipeCategoryDto recipeCategoryDto);
}
