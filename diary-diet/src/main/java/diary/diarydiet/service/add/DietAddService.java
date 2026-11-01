package diary.diarydiet.service.add;

import diary.diarydiet.entity.dto.DietRecordAddDTO;
import diary.common.result.ApiResponse;

public interface DietAddService {
    ApiResponse<Long> addDietRecord(DietRecordAddDTO dietRecordAddDTO, Long userId);
}
