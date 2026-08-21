package diary.diaryinfo.service;

import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.entity.sysInfo.vo.CookWayVo;
import diary.common.entity.sysInfo.vo.IngredientCategoryVo;
import diary.common.entity.sysInfo.vo.IngredientVo;
import diary.common.result.ApiResponse;

import java.util.List;

public interface SysInfoQueryService {
    ApiResponse<List<IngredientCategoryVo>> getIngredientCategories(Integer isMain);

    ApiResponse<List<IngredientVo>> getIngredientsByCategory(IngredientReqDto ingredientReqDto);

    ApiResponse<List<CookWayVo>> getCookWays();
}
