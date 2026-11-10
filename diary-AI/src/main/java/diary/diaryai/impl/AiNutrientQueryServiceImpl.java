package diary.diaryai.impl;

import diary.common.entity.ai.vo.AiNutrientVO;
import diary.common.exception.ParamIllegalException;
import diary.diaryai.entity.ai.po.AiNutrientPO;
import diary.diaryai.mapper.AiNutrientQueryMapper;
import diary.diaryai.service.AiNutrientQueryService;
import diary.utils.common.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiNutrientQueryServiceImpl implements AiNutrientQueryService {
    private static final int MAX_BATCH_SIZE = 200;

    private final AiNutrientQueryMapper aiNutrientQueryMapper;

    @Override
    public List<AiNutrientVO> getLatestDietNutrients(List<Long> dietIds, Long userId) {
        try {
            MyUtils.check()
                    .notNull(userId, "userId")
                    .notNull(dietIds, "dietIds")
                    .listNotEmpty(dietIds, "dietIds")
                    .listNotContainsEmpty(dietIds, "dietIds");
        } catch (IllegalArgumentException exception) {
            throw new ParamIllegalException(exception.getMessage());
        }

        List<Long> distinctDietIds = dietIds.stream().distinct().toList();
        if (distinctDietIds.size() > MAX_BATCH_SIZE) {
            throw new ParamIllegalException("dietIds 单次最多查询200条");
        }

        return aiNutrientQueryMapper.selectLatestDietNutrients(userId, distinctDietIds).stream()
                .map(this::convertToVO)
                .toList();
    }

    private AiNutrientVO convertToVO(AiNutrientPO nutrient) {
        return AiNutrientVO.builder()
                .universalId(nutrient.getUniversalId())
                .aiTaskId(nutrient.getAiTaskId())
                .calory(nutrient.getCalory())
                .protein(nutrient.getProtein())
                .fat(nutrient.getFat())
                .carbohydrate(nutrient.getCarbohydrate())
                .sugar(nutrient.getSugar())
                .sodium(nutrient.getSodium())
                .createTime(nutrient.getCreateTime())
                .build();
    }
}
