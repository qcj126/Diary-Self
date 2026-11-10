package diary.diaryai.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.common.exception.NullResultException;
import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentInputSnapshotDTO;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentSubmitDTO;
import diary.diaryai.enums.aienum.AiFlagEnum;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static diary.diaryai.utils.CanonicalFormUtil.canonicalLoveSentimentForm;
import static diary.diaryai.utils.CanonicalFormUtil.canonicalNutrientForm;
import static diary.utils.common.MyUtils.convertJsonToClass;

/**
 * 为幂等校验生成稳定的请求快照和指纹。
 */
@Component
@RequiredArgsConstructor
public class AiRequestFingerprint {

    public String fingerprint(Object request, String aiFlag) {
        try {
            String value = null;
            if (aiFlag.equals(AiFlagEnum.RECIPE.name()) || aiFlag.equals(AiFlagEnum.DIET.name())) {
                AiInvokeDTO aiInvokeDTO = (AiInvokeDTO) request;
                value = MyUtils.writeJson(canonicalNutrientForm(aiInvokeDTO), "格式化aiInvokeDTO为Map失败");
            } else if (aiFlag.equals(AiFlagEnum.LOVE_RECORD.name())) {
                LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO = (LoveRecordSentimentSubmitDTO) request;
                value = MyUtils.writeJson(canonicalLoveSentimentForm(loveRecordSentimentSubmitDTO), "格式化loveRecordSentimentSubmitDTO为Map失败");
            }

            if (value == null) throw new NullResultException("json转字符串异常，value为空");

            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM不支持SHA-256", e);
        }
    }

    public String fingerprintSnapshot(String snapshot, String aiFlag, String clientRequestId) {
        String fingerprint = null;
        if (aiFlag.equals(AiFlagEnum.RECIPE.name()) || aiFlag.equals(AiFlagEnum.DIET.name())) {
            fingerprint = fingerprint(convertJsonToClass(snapshot, AiInvokeDTO.class), aiFlag);
        } else if (aiFlag.equals(AiFlagEnum.LOVE_RECORD.name())) {
            LoveRecordSentimentInputSnapshotDTO snapshotDTO = (LoveRecordSentimentInputSnapshotDTO) convertJsonToClass(snapshot, LoveRecordSentimentInputSnapshotDTO.class);
            LoveRecordSentimentSubmitDTO request = new LoveRecordSentimentSubmitDTO();
            request.setAiType(snapshotDTO.getAiType());
            request.setLoveRecordId(snapshotDTO.getLoveRecord().getLoveRecordId());
            request.setClientRequestId(clientRequestId);
            fingerprint = fingerprint(request, aiFlag);
        }
        if (fingerprint == null) throw new NullResultException("无法获取快照结果");
        return fingerprint;
    }


}
