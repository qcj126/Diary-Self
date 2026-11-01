package diary.diarydiet.entity.convert;

import diary.diarydiet.entity.po.DietRecordPO;
import diary.diarydiet.entity.vo.DietRecordVO;
import diary.diarydiet.enums.MealTypeEnum;

public final class POConvertToVO {
    private POConvertToVO() {
    }
    
    /**
     * PO转VO
     */
    public static DietRecordVO convertToVO(DietRecordPO po) {
        return DietRecordVO.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .eatTime(po.getEatTime())
                .mealType(po.getMealType())
                .mealTypeName(MealTypeEnum.getNameByCode(po.getMealType()))
                .foodName(po.getFoodName())
                .location(po.getLocation())
                .note(po.getNote())
                .imageUrl(po.getImageUrl())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }
}
