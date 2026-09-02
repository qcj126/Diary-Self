package diary.diaryinfo.impl.queryserviceImpl;

import diary.common.convert.love.PoConvertToVo;
import diary.common.entity.love.vo.LoveMoodVO;
import diary.common.entity.love.vo.LoveTagVO;
import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.DiaryLoveMapper;
import diary.diaryinfo.service.queryservice.DiaryLoveQueryService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryLoveQueryServiceImpl implements DiaryLoveQueryService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    public ApiResponse<List<LoveMoodVO>> queryMoods(Boolean enabled) {
        return ApiResponse.success(diaryLoveMapper.selectLoveMoods(enabled).stream()
                .map(PoConvertToVo::convertToVo)
                .toList());
    }

    @Override
    public ApiResponse<List<LoveTagVO>> queryTags(Long coupleId) {
        MyUtils.check().notNull(coupleId, "coupleId");
        return ApiResponse.success(diaryLoveMapper.selectLoveTagsByCoupleId(coupleId).stream()
                .map(PoConvertToVo::convertToVo)
                .toList());
    }
}
