package diary.common.convert.love;

import diary.common.entity.love.po.LoveCouplePO;
import diary.common.entity.love.vo.LoveCoupleVO;

public class PoConvertToVo {
    public static LoveCoupleVO convertToVo(LoveCouplePO loveCouplePO) {
        LoveCoupleVO loveCoupleVO = new LoveCoupleVO();
        loveCoupleVO.setId(loveCouplePO.getId());
        loveCoupleVO.setOwnerUserId(loveCouplePO.getOwnerUserId());
        loveCoupleVO.setPartnerUserId(loveCouplePO.getPartnerUserId());
        loveCoupleVO.setPartnerName(loveCouplePO.getPartnerName());
        loveCoupleVO.setStartDate(loveCouplePO.getStartDate());
        loveCoupleVO.setStatus(loveCouplePO.getStatus());
        loveCoupleVO.setCreateTime(loveCouplePO.getCreateTime());
        loveCoupleVO.setUpdateTime(loveCouplePO.getUpdateTime());
        return loveCoupleVO;
    }
}
