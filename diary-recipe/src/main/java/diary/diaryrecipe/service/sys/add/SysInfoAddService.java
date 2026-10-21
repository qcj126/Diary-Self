package diary.diaryrecipe.service.sys.add;

import diary.common.result.ApiResponse;
import diary.diaryrecipe.entity.dto.IngredientReqDto;

public interface SysInfoAddService {
    ApiResponse<?> addIngredient(IngredientReqDto ingredientReqDto);
}
