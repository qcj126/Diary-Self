package diary.common.convert.diet;

import diary.common.enums.dietenum.MealTypeEnum;
import diary.common.entity.diet.po.DietRecordPO;
import diary.common.entity.diet.vo.DietRecordVO;

public class POConvertToVO {
    
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
                .calories(po.getCalories())
                .protein(po.getProtein())
                .fat(po.getFat())
                .carbohydrate(po.getCarbohydrate())
                .fullnessScore(po.getFullnessScore())
                .location(po.getLocation())
                .note(po.getNote())
                .createTime(po.getCreatedAt())
                .updateTime(po.getUpdatedAt())
                .build();
    }
}
