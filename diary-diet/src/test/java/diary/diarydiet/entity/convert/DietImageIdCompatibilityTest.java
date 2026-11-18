package diary.diarydiet.entity.convert;

import diary.common.exception.ParamIllegalException;
import diary.diarydiet.entity.dto.DietRecordAddDTO;
import diary.diarydiet.entity.dto.DietRecordDTO;
import diary.diarydiet.support.DietRecordValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DietImageIdCompatibilityTest {

    @Test
    void addRequestDefaultsImageIdToZeroForExistingClients() {
        DietRecordAddDTO request = validAddRequest();

        assertEquals(0L, request.getImageId());
        assertEquals(0L, DTOConvertToPO.dietRecordAddDTOConvertToPO(request, 1L, 2L).getImageId());
    }

    @Test
    void updateRequestKeepsNullSoOldClientsDoNotClearExistingImageRelation() {
        DietRecordDTO request = validUpdateRequest();

        assertNull(request.getImageId());
        assertNull(DTOConvertToPO.dietRecordDTOConvertToPO(request, request.getId()).getImageId());
    }

    @Test
    void negativeImageIdIsRejected() {
        DietRecordAddDTO request = validAddRequest();
        request.setImageId(-1L);

        assertThrows(ParamIllegalException.class, () -> DietRecordValidator.validateForAdd(request, 2L));
    }

    private DietRecordAddDTO validAddRequest() {
        DietRecordAddDTO request = new DietRecordAddDTO();
        request.setFoodName("测试餐");
        request.setEatTime(LocalDateTime.of(2026, 11, 18, 12, 0));
        request.setMealType((byte) 20);
        request.setLocation("home");
        return request;
    }

    private DietRecordDTO validUpdateRequest() {
        DietRecordDTO request = new DietRecordDTO();
        request.setId(1L);
        request.setUserId(2L);
        request.setFoodName("测试餐");
        request.setEatTime(LocalDateTime.of(2026, 11, 18, 12, 0));
        request.setMealType((byte) 20);
        request.setLocation("home");
        return request;
    }
}
