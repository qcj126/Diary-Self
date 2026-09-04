package diary.diaryinfo.service.addservice;

import diary.common.entity.love.dto.*;
import diary.common.result.ApiResponse;

public interface DiaryLoveAddService {
    ApiResponse<String> addMood(LoveMoodDTO dto);
}
