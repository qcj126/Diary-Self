package diary.diarylove.impl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.convert.love.PoConvertToVo;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.po.LoveCouplePO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.DiaryLoveQueryService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiaryLoveQueryServiceImpl implements DiaryLoveQueryService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    public ApiResponse<LoveCoupleVO> queryCouples(LoveCoupleDTO loveCoupleDTO) {
        // status默认为1
        MyUtils.check()
                .notNull(loveCoupleDTO, "loveCoupleDTO")
                .notNull(loveCoupleDTO.getOwnerUserId(), "ownerUserId");
        if (loveCoupleDTO.getStatus() == null) loveCoupleDTO.setStatus(1);
        LoveCouplePO loveCouplePO = diaryLoveMapper.queryCouples(loveCoupleDTO);
        if (loveCouplePO == null) return ApiResponse.queryFail();
        LoveCoupleVO loveCoupleVO = PoConvertToVo.convertToVo(loveCouplePO);
        return ApiResponse.success(loveCoupleVO);
    }
}
