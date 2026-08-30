package diary.diarylove.service;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveAnniversaryDTO;
import diary.common.entity.love.dto.LoveLocationDTO;
import diary.common.entity.love.dto.LoveMoodDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.dto.LoveRecordImageDTO;
import diary.common.entity.love.dto.LoveRecordMoodDTO;
import diary.common.entity.love.dto.LoveRecordTagDTO;
import diary.common.entity.love.dto.LoveTagDTO;
import diary.common.result.ApiResponse;

public interface DiaryLoveAddService {
    ApiResponse<String> addCouples(LoveCoupleDTO loveCoupleDTO);

    ApiResponse<String> addAnniversary(LoveAnniversaryDTO dto);

    ApiResponse<String> addLocation(LoveLocationDTO dto);

    ApiResponse<String> addRecord(LoveRecordDTO dto);

    ApiResponse<String> addRecordImage(LoveRecordImageDTO dto);

    ApiResponse<String> addMood(LoveMoodDTO dto);

    ApiResponse<String> addRecordMood(LoveRecordMoodDTO dto);

    ApiResponse<String> addTag(LoveTagDTO dto);

    ApiResponse<String> addRecordTag(LoveRecordTagDTO dto);
}
