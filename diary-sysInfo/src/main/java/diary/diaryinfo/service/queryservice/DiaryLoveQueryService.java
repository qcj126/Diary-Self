package diary.diaryinfo.service.queryservice;

import diary.common.entity.love.vo.LoveMoodVO;
import diary.common.entity.love.vo.LoveTagVO;
import diary.common.result.ApiResponse;

import java.util.List;

public interface DiaryLoveQueryService {
    ApiResponse<List<LoveMoodVO>> queryMoods(Boolean enabled);

    ApiResponse<List<LoveTagVO>> queryTags(Long coupleId);
}
