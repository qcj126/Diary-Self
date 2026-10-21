package diary.diaryrecipe.service.add;


import diary.diaryrecipe.entity.dto.RecipeCategoryDto;
import diary.diaryrecipe.entity.dto.RecipeReqDto;
import diary.common.result.ApiResponse;

public interface RecipeAddService {
    ApiResponse<String> addRecipe(RecipeReqDto recipeReqDto);

    ApiResponse<String> addCategory(RecipeCategoryDto recipeCategoryDto);
}
