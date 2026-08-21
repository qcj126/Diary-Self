package diary.diarylove.service;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.result.ApiResponse;

public interface DiaryLoveQueryService {
    ApiResponse<LoveCoupleVO> queryCouples(LoveCoupleDTO loveCoupleDTO);
}
