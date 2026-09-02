package diary.diaryinfo.service.updateservice;

import diary.common.entity.love.dto.LoveMoodDTO;
import diary.common.entity.love.dto.LoveTagDTO;
import diary.common.result.ApiResponse;

public interface DiaryLoveUpdateService {
    ApiResponse<String> updateMood(LoveMoodDTO dto);

    ApiResponse<String> updateTag(LoveTagDTO dto);
}
