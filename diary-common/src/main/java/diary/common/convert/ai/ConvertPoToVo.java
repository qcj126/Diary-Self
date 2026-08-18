package diary.common.convert.ai;

import diary.common.entity.ai.po.AiNutrientPO;
import diary.common.entity.ai.po.AiTaskPO;
import diary.common.entity.ai.vo.AiTaskResultVo;
import diary.common.entity.ai.vo.AiTaskStatusVo;

public class ConvertPoToVo {
    public static AiTaskStatusVo convertToVo(AiTaskPO aiTaskPO) {
        return AiTaskStatusVo.builder()
                .taskId(aiTaskPO.getId())
                .status(aiTaskPO.getStatus())
                .attemptCount(aiTaskPO.getAttemptCount())
                .maxAttempts(aiTaskPO.getMaxAttempts())
                .resultId(aiTaskPO.getAiInfoId())
                .errorCode(aiTaskPO.getErrorCode())
                .errorMessage(aiTaskPO.getErrorMessage())
                .createTime(aiTaskPO.getCreateTime())
                .queueTime(aiTaskPO.getQueueTime())
                .startTime(aiTaskPO.getStartTime())
                .finishTime(aiTaskPO.getFinishTime())
                .versionId(aiTaskPO.getVersionId())
                .build();
    }

    public static AiTaskResultVo convertToVo(AiNutrientPO nutrientPO, AiTaskPO aiTaskPO) {
        return AiTaskResultVo.builder()
                .taskId(nutrientPO.getId())
                .status(aiTaskPO.getStatus())
                .aiInfoId(nutrientPO.getAiInfoId())
                .universalId(nutrientPO.getUniversalId())
                .flag(nutrientPO.getFlag())
                .calory(nutrientPO.getCalory())
                .protein(nutrientPO.getProtein())
                .fat(nutrientPO.getFat())
                .carbohydrate(nutrientPO.getCarbohydrate())
                .sugar(nutrientPO.getSugar())
                .sodium(nutrientPO.getSodium())
                .errorCode(aiTaskPO.getErrorCode())
                .errorMessage(aiTaskPO.getErrorMessage())
                .build();
    }
}
