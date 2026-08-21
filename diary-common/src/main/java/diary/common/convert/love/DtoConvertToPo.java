package diary.common.convert.love;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.po.LoveCouplePO;

public class DtoConvertToPo {
    public static LoveCouplePO convertToPo(LoveCoupleDTO loveCoupleDTO) {
        LoveCouplePO loveCouplePO = new LoveCouplePO();
        loveCouplePO.setId(loveCoupleDTO.getId());
        loveCouplePO.setOwnerUserId(loveCoupleDTO.getOwnerUserId());
        loveCouplePO.setPartnerUserId(loveCoupleDTO.getPartnerUserId());
        loveCouplePO.setPartnerName(loveCoupleDTO.getPartnerName());
        loveCouplePO.setStartDate(loveCoupleDTO.getStartDate());
        loveCouplePO.setStatus(loveCoupleDTO.getStatus());
        return loveCouplePO;
    }
}
