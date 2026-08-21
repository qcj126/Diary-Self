package diary.diarylove.service;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.result.ApiResponse;

public interface DiaryLoveAddService {
    ApiResponse<String> addCouples(LoveCoupleDTO loveCoupleDTO);
}
