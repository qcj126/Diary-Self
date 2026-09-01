package diary.diaryinfo.service.addservice;

import diary.common.entity.sysInfo.dto.IngredientReqDto;
import diary.common.result.ApiResponse;

public interface SysInfoAddService {
    ApiResponse<?> addIngredient(IngredientReqDto ingredientReqDto);
}
