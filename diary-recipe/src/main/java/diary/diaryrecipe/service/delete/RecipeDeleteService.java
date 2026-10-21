package diary.diaryrecipe.service.delete;

import diary.diaryrecipe.entity.dto.RecipeCategoryDto;
import diary.diaryrecipe.entity.dto.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeDeleteService {
    ApiResponse<String> deleteRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> deleteCategory(RecipeCategoryDto recipeCategoryDto);
}
