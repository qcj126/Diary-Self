package diary.diaryrecipe.service.sys.query;

import diary.common.result.ApiResponse;
import diary.diaryrecipe.entity.dto.IngredientReqDto;
import diary.diaryrecipe.entity.vo.CookWayVo;
import diary.diaryrecipe.entity.vo.IngredientCategoryVo;
import diary.diaryrecipe.entity.vo.sys.IngredientVo;

import java.util.List;

public interface SysInfoQueryService {
    ApiResponse<List<IngredientCategoryVo>> getIngredientCategories(Integer isMain);

    ApiResponse<List<IngredientVo>> getIngredientsByCategory(IngredientReqDto ingredientReqDto);

    ApiResponse<List<CookWayVo>> getCookWays();
}
