package diary.diarydiet.impl.add;

import diary.common.result.ApiResponse;
import diary.diarydiet.entity.dto.DietRecordAddDTO;
import diary.diarydiet.entity.po.DietRecordPO;
import diary.diarydiet.mapper.DietMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietAddServiceImplTest {
    @Mock
    private DietMapper dietMapper;
    @InjectMocks
    private DietAddServiceImpl service;

    @Test
    void shouldReturnCreatedDietIdAndPersistOnlyDietFields() {
        DietRecordAddDTO request = new DietRecordAddDTO();
        request.setFoodName("香煎三文鱼");
        request.setEatTime(LocalDateTime.of(2026, 11, 1, 12, 30));
        request.setMealType((byte) 20);
        request.setLocation("home");
        request.setImageUrl("/file/images/diet.jpg");
        request.setNote("少油");
        when(dietMapper.insert(any(DietRecordPO.class))).thenReturn(1);

        ApiResponse<Long> response = service.addDietRecord(request, 10001L);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        ArgumentCaptor<DietRecordPO> captor = ArgumentCaptor.forClass(DietRecordPO.class);
        verify(dietMapper).insert(captor.capture());
        assertEquals(response.getData(), captor.getValue().getId());
        assertEquals(10001L, captor.getValue().getUserId());
        assertEquals("香煎三文鱼", captor.getValue().getFoodName());
    }
}
