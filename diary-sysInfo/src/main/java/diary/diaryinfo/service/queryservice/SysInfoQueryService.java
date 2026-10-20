package diary.diaryinfo.service.queryservice;

import diary.diaryinfo.entity.dto.IngredientReqDto;
import diary.diaryinfo.entity.vo.CookWayVo;
import diary.diaryinfo.entity.vo.IngredientCategoryVo;
import diary.diaryinfo.entity.vo.IngredientVo;
import diary.common.result.ApiResponse;

import java.util.List;

public interface SysInfoQueryService {
    ApiResponse<List<IngredientCategoryVo>> getIngredientCategories(Integer isMain);

    ApiResponse<List<IngredientVo>> getIngredientsByCategory(IngredientReqDto ingredientReqDto);

    ApiResponse<List<CookWayVo>> getCookWays();
}
