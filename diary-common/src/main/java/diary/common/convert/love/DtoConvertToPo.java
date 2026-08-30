package diary.common.convert.love;

import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.po.LoveCouplePO;

public class DtoConvertToPo {
    public static LoveCouplePO convertToPo(LoveCoupleDTO loveCoupleDTO) {
        return LoveEntityConverter.toPo(loveCoupleDTO);
    }
}
