package diary.common.convert.love;

import diary.common.entity.love.po.LoveCouplePO;
import diary.common.entity.love.vo.LoveCoupleVO;

public class PoConvertToVo {
    public static LoveCoupleVO convertToVo(LoveCouplePO loveCouplePO) {
        return LoveEntityConverter.toVo(loveCouplePO);
    }
}
