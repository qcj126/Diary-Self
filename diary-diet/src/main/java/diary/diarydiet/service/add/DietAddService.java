package diary.diarydiet.service.add;

import diary.diarydiet.entity.dto.DietRecordDTO;
import diary.common.result.ApiResponse;

public interface DietAddService {
    ApiResponse<String> addDietRecord(DietRecordDTO dietRecordDTO);
}
