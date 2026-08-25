package diary.diaryinfo.service;

import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.result.ApiResponse;

public interface SysInfoAddService {
    ApiResponse<?> addIngredient(IngredientReqDto ingredientReqDto);
}
