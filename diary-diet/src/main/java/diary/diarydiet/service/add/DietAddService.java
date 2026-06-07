package diary.diarydiet.service.add;

import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.result.ApiResponse;

public interface DietAddService {
    ApiResponse<String> addDietRecord(DietRecordDTO dietRecordDTO);
}
