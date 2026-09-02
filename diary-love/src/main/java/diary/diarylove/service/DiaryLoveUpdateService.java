package diary.diarylove.service;

import diary.common.entity.love.dto.*;
import diary.common.result.ApiResponse;

public interface DiaryLoveUpdateService {
    ApiResponse<String> updateCouple(LoveCoupleDTO dto);

    ApiResponse<String> updateAnniversary(LoveAnniversaryDTO dto);

    ApiResponse<String> updateLocation(LoveLocationDTO dto);

    ApiResponse<String> updateRecord(UpdateLoveRecordDto dto);

    ApiResponse<String> updateRecordImage(LoveRecordImageDTO dto);
}
