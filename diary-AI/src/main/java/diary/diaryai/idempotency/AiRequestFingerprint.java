package diary.diaryai.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.common.exception.NullResultException;
import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentInputSnapshotDTO;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentSubmitDTO;
import diary.diaryai.enums.aienum.AiFlagEnum;
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

    public String fingerprint(Object request, String aiFlag) {
        try {
            String value = null;
            if (aiFlag.equals(AiFlagEnum.RECIPE.name()) || aiFlag.equals(AiFlagEnum.DIET.name())) {
                AiInvokeDTO aiInvokeDTO = (AiInvokeDTO) request;
                value = objectMapper.writeValueAsString(canonicalNutrientForm(aiInvokeDTO));
            } else if (aiFlag.equals(AiFlagEnum.LOVE_RECORD.name())) {
                LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO = (LoveRecordSentimentSubmitDTO) request;
                value = objectMapper.writeValueAsString(canonicalLoveSentimentForm(loveRecordSentimentSubmitDTO));
            }

            if (value == null) throw new NullResultException("json转字符串异常，value为空");

            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM不支持SHA-256", e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> canonicalLoveSentimentForm(LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO) {
        Map<String, Object> value = new TreeMap<>();
        value.put("aiType", loveRecordSentimentSubmitDTO.getAiType());
        value.put("clientRequestId", loveRecordSentimentSubmitDTO.getClientRequestId());
        value.put("loveRecordId", loveRecordSentimentSubmitDTO.getLoveRecordId());
        return value;
    }

    public String fingerprintSnapshot(String snapshot, String aiFlag, String clientRequestId) {
        try {
            String fingerprint = null;
            if (aiFlag.equals(AiFlagEnum.RECIPE.name()) || aiFlag.equals(AiFlagEnum.DIET.name())) {
                fingerprint = fingerprint(objectMapper.readValue(snapshot, AiInvokeDTO.class), aiFlag);
            } else if (aiFlag.equals(AiFlagEnum.LOVE_RECORD.name())) {
                LoveRecordSentimentInputSnapshotDTO snapshotDTO = objectMapper.readValue(snapshot, LoveRecordSentimentInputSnapshotDTO.class);
                LoveRecordSentimentSubmitDTO request = new LoveRecordSentimentSubmitDTO();
                request.setAiType(snapshotDTO.getAiType());
                request.setLoveRecordId(snapshotDTO.getLoveRecord().getLoveRecordId());
                request.setClientRequestId(clientRequestId);
                fingerprint = fingerprint(request, aiFlag);
            }
            if (fingerprint == null) throw new NullResultException("无法获取快照结果");
            return fingerprint;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("已存储的AI任务快照无法校验", e);
        }
    }

    public Map<String, Object> canonicalNutrientForm(AiInvokeDTO request) {
        Map<String, Object> value = new TreeMap<>();
        value.put("aiType", request.getAiType());
        value.put("clientRequestId", request.getClientRequestId());
        value.put("cookWay", request.getCookWay());
        value.put("ingredients", request.getIngredients() == null ? null : new TreeMap<>(request.getIngredients()));
        value.put("universalId", request.getUniversalId());
        value.put("flag", request.getFlag());
        return value;
    }

    public Map<String, Object> canonicalLoveRecordSentimentSnapshotForm(LoveRecordSentimentInputSnapshotDTO snapshotDTO) {
        Map<String, Object> value = new TreeMap<>();
        value.put("aiType", snapshotDTO.getAiType());
        value.put("snapshotVersion", snapshotDTO.getSnapshotVersion());

        LoveRecordAiSnapshotDTO loveRecord = snapshotDTO.getLoveRecord();
        Map<String, Object> loveRecordMap = new TreeMap<>();
        loveRecordMap.put("loveRecordId", loveRecord.getLoveRecordId());
        loveRecordMap.put("content", loveRecord.getContent());
        loveRecordMap.put("categoryCode", loveRecord.getCategoryCode());
        loveRecordMap.put("recordDate", loveRecord.getRecordDate());
        loveRecordMap.put("title", loveRecord.getTitle());

        value.put("loveRecord", loveRecordMap);
        return value;
    }
}
