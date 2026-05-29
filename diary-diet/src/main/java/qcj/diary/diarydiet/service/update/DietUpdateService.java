package qcj.diary.diarydiet.service.update;

import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.result.ApiResponse;

public interface DietUpdateService {
    ApiResponse<String> updateDietRecord(DietRecordDTO dietRecordDTO);
}
