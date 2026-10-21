package diary.diaryrecipe.service.update;

import diary.diaryrecipe.entity.dto.RecipeCategoryDto;
import diary.diaryrecipe.entity.dto.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeUpdateService {
    ApiResponse<String> updateRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> updateCategory(RecipeCategoryDto recipeCategoryDto);
}
