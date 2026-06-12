package diary.common.convert.timemachine;

import diary.common.entity.timemachine.dto.TimeCardDTO;
import diary.common.entity.timemachine.dto.TimeCategoryDTO;
import diary.common.entity.timemachine.po.TimeCardPO;
import diary.common.entity.timemachine.po.TimeCategoryPO;

import java.sql.Time;

public class DtoConvertToPo {
    public static TimeCategoryPO convertToPO(TimeCategoryDTO dto) {
        TimeCategoryPO timeCategoryPO = new TimeCategoryPO();
        timeCategoryPO.setId(dto.getId());
        timeCategoryPO.setUserId(dto.getUserId());
        timeCategoryPO.setCategoryName(dto.getCategoryName());
        return timeCategoryPO;
    }

    public static TimeCardPO convertToPO(TimeCardDTO dto) {
        TimeCardPO timeCardPO = new TimeCardPO();
        timeCardPO.setId(dto.getId());
        timeCardPO.setUserId(dto.getUserId());
        timeCardPO.setCardTitle(dto.getCardTitle());
        timeCardPO.setCardContent(dto.getCardContent());
        timeCardPO.setCategoryId(dto.getCategoryId());
        timeCardPO.setImageId(dto.getImageId());
        timeCardPO.setRecordTime(dto.getRecordTime());
        return timeCardPO;
    }
}
