package diary.diaryai.utils;

import diary.common.entity.feign.LoveRecordAiSnapshotDTO;
import diary.diaryai.entity.ai.dto.AiInvokeDTO;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentInputSnapshotDTO;
import diary.diaryai.entity.ai.dto.LoveRecordSentimentSubmitDTO;

import java.util.Map;
import java.util.TreeMap;

public class CanonicalFormUtil {
    public static Map<String, Object> canonicalNutrientForm(AiInvokeDTO request) {
        Map<String, Object> value = new TreeMap<>();
        value.put("aiType", request.getAiType());
        value.put("clientRequestId", request.getClientRequestId());
        value.put("cookWay", request.getCookWay());
        value.put("ingredients", request.getIngredients() == null ? null : new TreeMap<>(request.getIngredients()));
        value.put("universalId", request.getUniversalId());
        value.put("flag", request.getFlag());
        return value;
    }

    public static Map<String, Object> canonicalLoveSentimentForm(LoveRecordSentimentSubmitDTO loveRecordSentimentSubmitDTO) {
        Map<String, Object> value = new TreeMap<>();
        value.put("aiType", loveRecordSentimentSubmitDTO.getAiType());
        value.put("clientRequestId", loveRecordSentimentSubmitDTO.getClientRequestId());
        value.put("loveRecordId", loveRecordSentimentSubmitDTO.getLoveRecordId());
        return value;
    }

    public static Map<String, Object> canonicalLoveRecordSentimentSnapshotForm(LoveRecordSentimentInputSnapshotDTO snapshotDTO) {
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
