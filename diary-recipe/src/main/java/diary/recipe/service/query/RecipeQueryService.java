package diary.recipe.service.query;

import diary.common.entity.recipe.dto.RecipePageReqDto;
import diary.common.entity.recipe.vo.PageRecipeVO;
import diary.common.entity.recipe.vo.RecipeCategoryVO;
import diary.common.entity.recipe.vo.RecipeVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface RecipeQueryService {
    PageRecipeVO<RecipeVO> pageQueryRecipe(RecipePageReqDto pageReqDto);

    ApiResponse<List<RecipeCategoryVO>> queryRecipeCategory();
}
