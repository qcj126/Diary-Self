package diary.diaryai.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * 为幂等校验生成稳定的请求快照和指纹。
 */
@Component
@RequiredArgsConstructor
public class AiRequestFingerprint {
    private final ObjectMapper objectMapper;

    public String fingerprint(AiInvokeDTO request) {
        try {
            String value = objectMapper.writeValueAsString(canonicalForm(request));
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM不支持SHA-256", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String fingerprintSnapshot(String snapshot) {
        try {
            return fingerprint(objectMapper.readValue(snapshot, AiInvokeDTO.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("已存储的AI任务快照无法校验", e);
        }
    }

    public Map<String, Object> canonicalForm(AiInvokeDTO request) {
        Map<String, Object> value = new TreeMap<>();
        value.put("aiApplication", request.getAiApplication());
        value.put("aiType", request.getAiType());
        value.put("clientRequestId", request.getClientRequestId());
        value.put("cookWay", request.getCookWay());
        value.put("flag", request.getFlag());
        value.put("ingredients", request.getIngredients() == null ? null : new TreeMap<>(request.getIngredients()));
        value.put("universalId", request.getUniversalId());
        return value;
    }
}
