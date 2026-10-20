package diary.diarylove.service;

import diary.common.entity.love.dto.*;
import diary.common.result.ApiResponse;
import diary.diarylove.entity.dto.AddLoveRecordDto;
import diary.diarylove.entity.dto.LoveAnniversaryDTO;
import diary.diarylove.entity.dto.LoveCoupleDTO;

public interface DiaryLoveAddService {
    ApiResponse<String> addCouples(LoveCoupleDTO dto);

    ApiResponse<String> addAnniversary(LoveAnniversaryDTO dto);

    ApiResponse<String> addRecord(AddLoveRecordDto dto);
}
