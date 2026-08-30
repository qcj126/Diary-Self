package diary.diarylove.service;

import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.vo.LoveAnniversaryVO;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.entity.love.vo.LoveLocationVO;
import diary.common.entity.love.vo.LoveMoodVO;
import diary.common.entity.love.vo.LoveRecordImageVO;
import diary.common.entity.love.vo.LoveRecordMoodVO;
import diary.common.entity.love.vo.LoveRecordTagVO;
import diary.common.entity.love.vo.LoveRecordVO;
import diary.common.entity.love.vo.LoveTagVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface DiaryLoveQueryService {
    ApiResponse<LoveCoupleVO> queryCouples(LoveCoupleDTO loveCoupleDTO);

    ApiResponse<List<LoveAnniversaryVO>> queryAnniversaries(Long coupleId);

    ApiResponse<List<LoveLocationVO>> queryLocations(Long coupleId);

    ApiResponse<LoveRecordVO> queryRecord(Long id);

    ApiResponse<List<LoveRecordVO>> queryRecords(LoveRecordDTO query);

    ApiResponse<List<LoveRecordImageVO>> queryRecordImages(Long recordId);

    ApiResponse<List<LoveMoodVO>> queryMoods(Boolean enabled);

    ApiResponse<List<LoveRecordMoodVO>> queryRecordMoods(Long recordId);

    ApiResponse<List<LoveTagVO>> queryTags(Long coupleId);

    ApiResponse<List<LoveRecordTagVO>> queryRecordTags(Long recordId);
}
