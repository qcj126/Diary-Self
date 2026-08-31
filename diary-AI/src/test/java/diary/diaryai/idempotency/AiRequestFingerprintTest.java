package diary.diaryai.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.ai.dto.AiInvokeDTO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiRequestFingerprintTest {
    private final AiRequestFingerprint fingerprint = new AiRequestFingerprint(new ObjectMapper());

    @Test
    void mapOrderDoesNotChangeFingerprint() {
        AiInvokeDTO first = request(new LinkedHashMap<>(Map.of("rice", "100g", "egg", "1")));
        LinkedHashMap<String, String> reversed = new LinkedHashMap<>();
        reversed.put("rice", "100g");
        reversed.put("egg", "1");
        AiInvokeDTO second = request(reversed);

        assertThat(fingerprint.fingerprint(first)).isEqualTo(fingerprint.fingerprint(second));
        assertThat(fingerprint.canonicalSnapshot(first)).isEqualTo(fingerprint.canonicalSnapshot(second));
    }

    @Test
    void changedBusinessInputChangesFingerprint() {
        AiInvokeDTO first = request(Map.of("rice", "100g"));
        AiInvokeDTO second = request(Map.of("rice", "200g"));

        assertThat(fingerprint.fingerprint(first)).isNotEqualTo(fingerprint.fingerprint(second));
    }

    private AiInvokeDTO request(Map<String, String> materials) {
        AiInvokeDTO request = new AiInvokeDTO();
        request.setClientRequestId("request-1");
        request.setAiType(1);
        request.setAiApplication(1);
        request.setMaterials(materials);
        request.setCookWay("boil");
        request.setFlag("DIET");
        request.setUniversalId(10L);
        return request;
    }
}
