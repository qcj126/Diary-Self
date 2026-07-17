package diary.common.convert.timemachine;

import diary.common.entity.timemachine.po.TimeCategoryPO;
import diary.common.entity.timemachine.vo.TimeCategoryVO;

import java.util.Date;

public class PoConvertToVo {
    public static TimeCategoryVO convertToTimeCategoryVO(TimeCategoryPO timeCategoryPO) {
        if (timeCategoryPO == null) {
            return null;
        }
        return TimeCategoryVO.builder()
                .id(timeCategoryPO.getId())
                .userId(String.valueOf(timeCategoryPO.getUserId()))
                .categoryName(timeCategoryPO.getCategoryName())
                .categoryNum(timeCategoryPO.getCategoryNum())
                .sort(timeCategoryPO.getSort())
                .status(timeCategoryPO.getDeleted())
                .createTime(timeCategoryPO.getCreateTime() != null
                        ? Date.from(timeCategoryPO.getCreateTime()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())
                        : null)
                .build();
    }
}
