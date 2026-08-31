package diary.diaryai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiInvokeDTO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.exception.IdempotencyConflictException;
import diary.diaryai.idempotency.AiRequestFingerprint;
import diary.diaryai.mapper.DiaryAiMapper;
import diary.diaryai.redis.AiIdempotencyCacheService;
import diary.diaryai.redis.AiSubmitRateLimiter;
import diary.diaryai.redis.AiTaskCacheService;
import diary.diaryai.service.AiTaskCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiTaskApplicationServiceImplTest {
    @Mock private DiaryAiMapper mapper;
    @Mock private AiTaskCommandService commandService;
    @Mock private AiIdempotencyCacheService idempotencyCache;
    @Mock private AiSubmitRateLimiter rateLimiter;
    @Mock private AiTaskCacheService taskCache;

    private AiRequestFingerprint fingerprint;
    private AiTaskApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        fingerprint = new AiRequestFingerprint(new ObjectMapper());
        service = new AiTaskApplicationServiceImpl(
                mapper, commandService, idempotencyCache, rateLimiter, taskCache, fingerprint);
    }

    @Test
    void sameIdempotencyKeyWithDifferentPayloadReturnsConflict() {
        AiInvokeDTO original = request("100g");
        AiTaskPO existing = AiTaskPO.builder()
                .id(1L)
                .clientRequestId("request-1")
                .requestHash(fingerprint.fingerprint(original))
                .inputSnapshot(fingerprint.canonicalSnapshot(original))
                .build();
        when(idempotencyCache.get(9L, "request-1")).thenReturn(Optional.empty());
        when(mapper.selectByUserIdAndClientRequestId(9L, "request-1")).thenReturn(existing);

        assertThatThrownBy(() -> service.submitTask(request("200g"), 9L))
                .isInstanceOf(IdempotencyConflictException.class);

        verify(commandService, never()).createTaskAndOutbox(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong());
    }

    private AiInvokeDTO request(String amount) {
        AiInvokeDTO request = new AiInvokeDTO();
        request.setClientRequestId("request-1");
        request.setAiType(1);
        request.setAiApplication(1);
        request.setMaterials(Map.of("rice", amount));
        request.setCookWay("boil");
        request.setFlag("DIET");
        request.setUniversalId(10L);
        return request;
    }
}
