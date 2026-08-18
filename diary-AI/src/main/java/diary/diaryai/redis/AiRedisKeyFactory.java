package diary.diaryai.redis;

import diary.diaryai.properties.AiTaskProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class AiRedisKeyFactory {
    private final AiTaskProperties properties;

    public String task(Long taskId) {
        return properties.getCache().getKeyPrefix() + ":task:" + taskId;
    }

    public String idempotency(Long userId, String clientRequestId) {
        return properties.getCache().getKeyPrefix() + ":idem:" + userId + ":" + requestHash(clientRequestId);
    }

    public String submitRate(Long userId, long epochMinute) {
        return properties.getCache().getKeyPrefix() + ":submit:rate:" + userId + ":" + epochMinute;
    }

    private String requestHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 24);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }
}
