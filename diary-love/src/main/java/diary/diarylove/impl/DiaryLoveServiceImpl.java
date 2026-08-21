package diary.diarylove.impl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.po.LoveCouplePO;
import diary.common.entity.love.vo.LoveCoupleVO;
import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.DiaryLoveService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryLoveServiceImpl implements DiaryLoveService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    public Integer addCouples(LoveCoupleDTO loveCoupleDTO) {
        MyUtils.check()
                .notNull(loveCoupleDTO, "loveCoupleDTO")
                .notNull(loveCoupleDTO.getOwnerUserId(), "ownerUserId")
                .notNull(loveCoupleDTO.getPartnerUserId(), "partnerUserId")
                .notNull(loveCoupleDTO.getPartnerName(), "partnerName")
                .notEmpty(loveCoupleDTO.getPartnerName(), "partnerName")
                .notNull(loveCoupleDTO.getStartDate(), "startDate");
        loveCoupleDTO.setId(MyUtils.getPrimaryKey());
        LoveCouplePO loveCouplePO = DtoConvertToPo.convertToPo(loveCoupleDTO);
        return diaryLoveMapper.insertLoveCouples(loveCouplePO);
    }
}
