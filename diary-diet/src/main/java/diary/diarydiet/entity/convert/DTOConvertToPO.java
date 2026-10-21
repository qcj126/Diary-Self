package diary.diarydiet.entity.convert;

import diary.diarydiet.entity.dto.DietRecordDTO;
import diary.diarydiet.entity.po.DietRecordPO;

public final class DTOConvertToPO {
    private DTOConvertToPO() {
    }

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
                .sugar(dietRecordDTO.getSugar())
                .sodium(dietRecordDTO.getSodium())
                .fullnessScore(dietRecordDTO.getFullnessScore())
                .location(dietRecordDTO.getLocation())
                .note(dietRecordDTO.getNote())
                .imageUrl(dietRecordDTO.getImageUrl())
                .build();
    }
}
