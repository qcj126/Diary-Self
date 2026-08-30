package diary.diarylove.service;

import diary.common.entity.love.dto.LoveAnniversaryDTO;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveLocationDTO;
import diary.common.entity.love.dto.LoveMoodDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.dto.LoveRecordImageDTO;
import diary.common.entity.love.dto.LoveRecordMoodDTO;
import diary.common.entity.love.dto.LoveRecordTagDTO;
import diary.common.entity.love.dto.LoveTagDTO;
import diary.common.result.ApiResponse;

public interface DiaryLoveUpdateService {
    ApiResponse<String> updateCouple(LoveCoupleDTO dto);

    ApiResponse<String> updateAnniversary(LoveAnniversaryDTO dto);

    ApiResponse<String> updateLocation(LoveLocationDTO dto);

    ApiResponse<String> updateRecord(LoveRecordDTO dto);

    ApiResponse<String> updateRecordImage(LoveRecordImageDTO dto);

    ApiResponse<String> updateMood(LoveMoodDTO dto);

    ApiResponse<String> updateRecordMood(LoveRecordMoodDTO dto);

    ApiResponse<String> updateTag(LoveTagDTO dto);

    ApiResponse<String> updateRecordTag(LoveRecordTagDTO dto);
}
