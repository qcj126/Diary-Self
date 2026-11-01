package diary.diaryai.impl;

import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.po.AiTaskPO;
import diary.diaryai.feignclient.DietClient;
import diary.diaryai.feignclient.RecipeClient;
import diary.diaryai.idempotency.AiRequestFingerprint;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.redis.AiSubmitRateLimiter;
import diary.diaryai.service.AiTransactionService;
import diary.diaryai.utils.MyUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTaskNutrientApplicationServiceImplTest {
    @Mock
    private DiaryAiMapper diaryAiMapper;
    @Mock
    private AiTransactionService aiTransactionService;
    @Mock
    private AiSubmitRateLimiter aiSubmitRateLimiter;
    @Mock
    private AiRequestFingerprint requestFingerprint;
    @Mock
    private RecipeClient recipeClient;
    @Mock
    private DietClient dietClient;
    @Mock
    private MyUtil myUtil;
    @InjectMocks
    private AiTaskNutrientApplicationServiceImpl service;

    @Test
    void shouldAcceptSupportedAiType() {
        AiInvokeDTO request = validRequest();
        AiTaskPO existingTask = AiTaskPO.builder().id(30001L).status("PENDING").build();
        when(requestFingerprint.fingerprint(request, "DIET")).thenReturn("hash");
        when(diaryAiMapper.selectByUserIdAndClientRequestId(10001L, "diet-request-1"))
                .thenReturn(existingTask);

        assertEquals(30001L, service.submitNutrientAnalyze(request, 10001L).getTaskId());
    }

    @Test
    void shouldRejectUnsupportedAiTypeAndEmptyCookWay() {
        AiInvokeDTO unsupported = validRequest();
        unsupported.setAiType(99);
        assertThrows(IllegalArgumentException.class,
                () -> service.submitNutrientAnalyze(unsupported, 10001L));

        AiInvokeDTO withoutCookWay = validRequest();
        withoutCookWay.setCookWay(" ");
        assertThrows(IllegalArgumentException.class,
                () -> service.submitNutrientAnalyze(withoutCookWay, 10001L));
    }

    private AiInvokeDTO validRequest() {
        AiInvokeDTO request = new AiInvokeDTO();
        request.setClientRequestId("diet-request-1");
        request.setAiType(1);
        request.setIngredients(Map.of("三文鱼", "150g"));
        request.setCookWay("香煎");
        request.setFlag("DIET");
        request.setUniversalId(20001L);
        return request;
    }
}
