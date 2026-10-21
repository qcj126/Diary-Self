package diary.diaryrecipe.service.query;

import diary.diaryrecipe.entity.dto.RecipePageReqDto;
import diary.diaryrecipe.entity.vo.PageRecipeVO;
import diary.diaryrecipe.entity.vo.RecipeCategoryVO;
import diary.diaryrecipe.entity.vo.RecipeVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface RecipeQueryService {
    ApiResponse<PageRecipeVO<RecipeVO>> pageQueryRecipe(RecipePageReqDto pageReqDto);

    ApiResponse<List<RecipeCategoryVO>> queryRecipeCategory();
}
