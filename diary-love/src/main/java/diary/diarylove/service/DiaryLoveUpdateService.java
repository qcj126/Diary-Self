package diary.diarylove.service;

import diary.common.result.ApiResponse;
import diary.diarylove.entity.dto.LoveAnniversaryDTO;
import diary.diarylove.entity.dto.LoveCoupleDTO;
import diary.diarylove.entity.dto.LoveLocationDTO;
import diary.diarylove.entity.dto.LoveMoodDTO;
import diary.diarylove.entity.dto.LoveRecordImageDTO;
import diary.diarylove.entity.dto.UpdateLoveRecordDto;

public interface DiaryLoveUpdateService {
    ApiResponse<String> updateCouple(LoveCoupleDTO dto);

    ApiResponse<String> updateAnniversary(LoveAnniversaryDTO dto);

    ApiResponse<String> updateLocation(LoveLocationDTO dto);

    ApiResponse<String> updateRecord(UpdateLoveRecordDto dto);

    ApiResponse<String> updateRecordImage(LoveRecordImageDTO dto);

    ApiResponse<String> updateMood(LoveMoodDTO dto);
}
