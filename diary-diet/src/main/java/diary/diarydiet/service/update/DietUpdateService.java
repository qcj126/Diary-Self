package diary.diarydiet.service.update;

import diary.diarydiet.entity.dto.DietRecordDTO;
import diary.common.result.ApiResponse;

public interface DietUpdateService {
    ApiResponse<String> updateDietRecord(DietRecordDTO dietRecordDTO);
}
