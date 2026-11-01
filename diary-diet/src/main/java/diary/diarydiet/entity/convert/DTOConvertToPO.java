package diary.diarydiet.entity.convert;

import diary.diarydiet.entity.dto.DietRecordAddDTO;
import diary.diarydiet.entity.dto.DietRecordDTO;
import diary.diarydiet.entity.po.DietRecordPO;

public final class DTOConvertToPO {
    private DTOConvertToPO() {
    }

    public static DietRecordPO dietRecordAddDTOConvertToPO(
            DietRecordAddDTO dietRecordAddDTO,
            Long id,
            Long userId
    ) {
        return DietRecordPO.builder()
                .id(id)
                .userId(userId)
                .eatTime(dietRecordAddDTO.getEatTime())
                .mealType(dietRecordAddDTO.getMealType())
                .foodName(dietRecordAddDTO.getFoodName())
                .location(dietRecordAddDTO.getLocation())
                .note(dietRecordAddDTO.getNote())
                .imageUrl(dietRecordAddDTO.getImageUrl())
                .build();
    }

    public static DietRecordPO dietRecordDTOConvertToPO(DietRecordDTO dietRecordDTO, Long id) {
        return DietRecordPO.builder()
                .id(id)
                .userId(dietRecordDTO.getUserId())
                .eatTime(dietRecordDTO.getEatTime())
                .mealType(dietRecordDTO.getMealType())
                .foodName(dietRecordDTO.getFoodName())
                .location(dietRecordDTO.getLocation())
                .note(dietRecordDTO.getNote())
                .imageUrl(dietRecordDTO.getImageUrl())
                .build();
    }
}
