package diary.common.convert.diet;

import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.entity.diet.po.DietRecordPO;

import java.time.LocalDateTime;

public class DTOConvertToPO {
    public static DietRecordPO dietRecordDTOConvertToPO(DietRecordDTO dietRecordDTO, Long id) {
        return DietRecordPO.builder()
                .id(id)
                .userId(dietRecordDTO.getUserId())
                .eatTime(dietRecordDTO.getEatTime())
                .mealType(dietRecordDTO.getMealType())
                .foodName(dietRecordDTO.getFoodName())
                .calories(dietRecordDTO.getCalories())
                .protein(dietRecordDTO.getProtein())
                .fat(dietRecordDTO.getFat())
                .carbohydrate(dietRecordDTO.getCarbohydrate())
                .fullnessScore(dietRecordDTO.getFullnessScore())
                .location(dietRecordDTO.getLocation())
                .note(dietRecordDTO.getNote())
                .deleted(false)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }
}
