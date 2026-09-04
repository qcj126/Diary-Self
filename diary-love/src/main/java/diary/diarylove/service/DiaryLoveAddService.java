package diary.diarylove.service;

import diary.common.entity.love.dto.*;
import diary.common.result.ApiResponse;

public interface DiaryLoveAddService {
    ApiResponse<String> addCouples(LoveCoupleDTO dto);

    ApiResponse<String> addAnniversary(LoveAnniversaryDTO dto);

    ApiResponse<String> addRecord(AddLoveRecordDto dto);
}
