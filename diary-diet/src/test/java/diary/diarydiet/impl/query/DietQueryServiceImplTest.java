package diary.diarydiet.impl.query;

import diary.common.entity.ai.vo.AiNutrientVO;
import diary.common.result.ApiResponse;
import diary.diarydiet.entity.po.DietRecordPO;
import diary.diarydiet.entity.vo.DietRecordVO;
import diary.diarydiet.feignclient.AiNutrientClient;
import diary.diarydiet.mapper.DietMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietQueryServiceImplTest {
    @Mock
    private DietMapper dietMapper;
    @Mock
    private AiNutrientClient aiNutrientClient;
    @InjectMocks
    private DietQueryServiceImpl service;

    @Test
    void shouldEnrichDietWithLatestAiNutrientValues() {
        Long dietId = 20001L;
        Long userId = 10001L;
        DietRecordPO record = DietRecordPO.builder()
                .id(dietId)
                .userId(userId)
                .mealType((byte) 20)
                .foodName("三文鱼饭")
                .build();
        AiNutrientVO nutrient = AiNutrientVO.builder()
                .universalId(dietId)
                .calory("520 kcal")
                .protein("32.5 g")
                .fat("18 g")
                .carbohydrate("62.4 g")
                .sugar("6 g")
                .sodium("780 mg")
                .build();
        when(dietMapper.selectByIdAndUserId(dietId, userId)).thenReturn(record);
        when(aiNutrientClient.getLatestDietNutrients(any(), any()))
                .thenReturn(ApiResponse.success(List.of(nutrient)));

        ApiResponse<DietRecordVO> response = service.getDietRecordById(dietId, userId);

        assertEquals(520, response.getData().getCalories());
        assertEquals(new BigDecimal("32.5"), response.getData().getProtein());
        assertEquals(new BigDecimal("780"), response.getData().getSodium());
    }
}
