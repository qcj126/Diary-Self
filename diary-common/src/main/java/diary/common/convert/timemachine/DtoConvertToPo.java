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
        timeCategoryPO.setCategoryNum(dto.getCategoryNum());
        timeCategoryPO.setStatus(dto.getStatus());
        timeCategoryPO.setCreateTime(dto.getCreateTime());
        timeCategoryPO.setSort(dto.getSort());

        return timeCategoryPO;
    }

    public static TimeCardPO convertToPO(TimeCardDTO dto) {
        TimeCardPO timeCardPO = new TimeCardPO();
        timeCardPO.setId(dto.getId());
        timeCardPO.setUserId(dto.getUserId());
        timeCardPO.setCardName(dto.getCardName());
        timeCardPO.setCardTitle(dto.getCardTitle());
        timeCardPO.setDescription(dto.getDescription());
        timeCardPO.setCategoryId(dto.getCategoryId());
        timeCardPO.setCategoryNum(dto.getCategoryNum());
        timeCardPO.setStatus(dto.getStatus());
        timeCardPO.setSort(dto.getSort());
        timeCardPO.setCreateTime(dto.getCreateTime());
        timeCardPO.setUpdateTime(dto.getUpdateTime());
        timeCardPO.setImageId(dto.getImageId());
        return timeCardPO;
    }
}
