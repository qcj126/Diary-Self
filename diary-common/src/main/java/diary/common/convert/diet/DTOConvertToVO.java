package diary.common.convert.diet;

import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.vo.DietRecordVO;
import diary.common.enums.dietenum.MealTypeEnum;

/**
 * 饮食记录 DTO 到 VO 的转换。
 */
public final class DTOConvertToVO {
    private DTOConvertToVO() {
    }

    public static DietRecordVO convertToVO(DietRecordDTO dto) {
        if (dto == null) {
            return null;
        }
        return DietRecordVO.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .eatTime(dto.getEatTime())
                .mealType(dto.getMealType())
                .mealTypeName(MealTypeEnum.getNameByCode(dto.getMealType()))
                .foodName(dto.getFoodName())
                .calories(dto.getCalories())
                .protein(dto.getProtein())
                .fat(dto.getFat())
                .carbohydrate(dto.getCarbohydrate())
                .sugar(dto.getSugar())
                .sodium(dto.getSodium())
                .fullnessScore(dto.getFullnessScore())
                .location(dto.getLocation())
                .note(dto.getNote())
                .imageUrl(dto.getImageUrl())
                .build();
    }
}
