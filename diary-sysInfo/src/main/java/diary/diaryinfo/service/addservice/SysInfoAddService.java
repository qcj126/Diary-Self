package diary.diaryinfo.service.addservice;

import diary.diaryinfo.entity.dto.IngredientReqDto;
import diary.common.result.ApiResponse;

public interface SysInfoAddService {
    ApiResponse<?> addIngredient(IngredientReqDto ingredientReqDto);
}
