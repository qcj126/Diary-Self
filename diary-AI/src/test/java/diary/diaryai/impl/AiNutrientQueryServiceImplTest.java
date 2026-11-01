package diary.diaryai.impl;

import diary.common.entity.ai.vo.AiNutrientVO;
import diary.common.exception.ParamIllegalException;
import diary.diaryai.entity.ai.po.AiNutrientPO;
import diary.diaryai.mapper.AiNutrientQueryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiNutrientQueryServiceImplTest {
    @Mock
    private AiNutrientQueryMapper aiNutrientQueryMapper;
    @InjectMocks
    private AiNutrientQueryServiceImpl service;

    @Test
    void shouldDeduplicateDietIdsAndReturnLatestResults() {
        AiNutrientPO nutrient = AiNutrientPO.builder()
                .universalId(20001L)
                .aiTaskId(30001L)
                .calory("500 kcal")
                .build();
        when(aiNutrientQueryMapper.selectLatestDietNutrients(10001L, List.of(20001L)))
                .thenReturn(List.of(nutrient));

        List<AiNutrientVO> result = service.getLatestDietNutrients(
                List.of(20001L, 20001L), 10001L);

        assertEquals(1, result.size());
        assertEquals(30001L, result.getFirst().getAiTaskId());
        verify(aiNutrientQueryMapper).selectLatestDietNutrients(10001L, List.of(20001L));
    }

    @Test
    void shouldRejectEmptyDietIds() {
        assertThrows(ParamIllegalException.class,
                () -> service.getLatestDietNutrients(List.of(), 10001L));
    }
}
