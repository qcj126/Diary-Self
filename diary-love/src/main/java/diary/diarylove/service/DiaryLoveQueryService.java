package diary.diarylove.service;

import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.diarylove.entity.dto.LoveRecordDTO;
import diary.diarylove.entity.dto.LoveCoupleDTO;
import diary.common.result.ApiResponse;
import diary.diarylove.entity.vo.LoveAnniversaryVO;
import diary.diarylove.entity.vo.LoveCoupleVO;
import diary.diarylove.entity.vo.LoveLocationVO;
import diary.diarylove.entity.vo.LoveMoodRecordVO;
import diary.diarylove.entity.vo.LoveMoodVO;
import diary.diarylove.entity.vo.LoveRecordImageVO;
import diary.diarylove.entity.vo.LoveRecordVO;

import java.util.List;

public interface DiaryLoveQueryService {
    ApiResponse<LoveCoupleVO> queryCouples(LoveCoupleDTO loveCoupleDTO);

    ApiResponse<List<LoveAnniversaryVO>> queryAnniversaries(Long coupleId);

    ApiResponse<List<LoveLocationVO>> queryLocations(Long coupleId);

    ApiResponse<LoveRecordVO> queryRecord(Long id);

    ApiResponse<List<LoveRecordVO>> queryRecords(LoveRecordDTO query);

    ApiResponse<List<LoveRecordImageVO>> queryRecordImages(Long recordId);

    ApiResponse<List<LoveMoodRecordVO>> queryRecordMoods(Long recordId);

    ApiResponse<List<LoveMoodVO>> queryMoods(Boolean enabled);

    ApiResponse<LoveRecordAiSnapshotDTO> queryLoveRecordSnapshot(Long loveRecordId, Long userId);
}
