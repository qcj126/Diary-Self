package diary.diarylove.service;

import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.vo.*;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface DiaryLoveQueryService {
    ApiResponse<LoveCoupleVO> queryCouples(LoveCoupleDTO loveCoupleDTO);

    ApiResponse<List<LoveAnniversaryVO>> queryAnniversaries(Long coupleId);

    ApiResponse<List<LoveLocationVO>> queryLocations(Long coupleId);

    ApiResponse<LoveRecordVO> queryRecord(Long id);

    ApiResponse<List<LoveRecordVO>> queryRecords(LoveRecordDTO query);

    ApiResponse<List<LoveRecordImageVO>> queryRecordImages(Long recordId);
}
