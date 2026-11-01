package diary.diarydiet.impl.query;

import diary.common.entity.ai.dto.AiNutrientBatchQueryDTO;
import diary.common.entity.ai.vo.AiNutrientVO;
import diary.common.exception.CustomException;
import diary.diarydiet.entity.convert.POConvertToVO;
import diary.diarydiet.entity.dto.DietQueryDTO;
import diary.diarydiet.entity.po.DietRecordPO;
import diary.diarydiet.entity.vo.DietRecordVO;
import diary.common.result.ApiResponse;
import diary.diarydiet.feignclient.AiNutrientClient;
import diary.diarydiet.support.DietRecordValidator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import diary.diarydiet.mapper.DietMapper;
import diary.diarydiet.service.query.DietQueryService;

import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DietQueryServiceImpl implements DietQueryService {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d[\\d,]*(?:\\.\\d+)?");

    @Resource
    private DietMapper dietMapper;
    @Resource
    private AiNutrientClient aiNutrientClient;

    @Override
    public ApiResponse<DietRecordVO> getDietRecordById(Long id, Long userId) {
        DietRecordValidator.validateId(id);

        DietRecordPO dietRecordPO = dietMapper.selectByIdAndUserId(id, userId);
        if (dietRecordPO == null) {
            return ApiResponse.queryFail();
        }

        DietRecordVO record = POConvertToVO.convertToVO(dietRecordPO);
        enrichLatestNutrients(List.of(record), userId);
        return ApiResponse.success(record);
    }

    @Override
    public ApiResponse<List<DietRecordVO>> getDietRecordsByUserId(Long userId) {
        DietQueryDTO query = new DietQueryDTO();
        query.setUserId(userId);
        return queryDietRecords(query);
    }

    @Override
    public ApiResponse<List<DietRecordVO>> queryDietRecords(DietQueryDTO query) {
        DietRecordValidator.validateQuery(query);
        if (query.getKeyword() != null) {
            query.setKeyword(query.getKeyword().trim());
        }
        List<DietRecordVO> records = dietMapper.selectList(query).stream()
                .map(POConvertToVO::convertToVO)
                .toList();
        enrichLatestNutrients(records, query.getUserId());
        return ApiResponse.success(records);
    }

    @Override
    public ApiResponse<String> queryFoodName(Long dietId, Long userId) {
        return ApiResponse.success(dietMapper.selectFoodNameByIdAndUserId(dietId, userId));
    }

    private void enrichLatestNutrients(List<DietRecordVO> records, Long userId) {
        if (records.isEmpty()) {
            return;
        }

        List<Long> dietIds = records.stream().map(DietRecordVO::getId).toList();
        List<AiNutrientVO> nutrients = new ArrayList<>();
        for (int start = 0; start < dietIds.size(); start += 200) {
            AiNutrientBatchQueryDTO request = new AiNutrientBatchQueryDTO();
            request.setDietIds(dietIds.subList(start, Math.min(start + 200, dietIds.size())));
            ApiResponse<List<AiNutrientVO>> response = aiNutrientClient.getLatestDietNutrients(userId, request);
            if (response == null || response.getCode() != 200 || response.getData() == null) {
                throw new CustomException("查询AI营养成分失败");
            }
            nutrients.addAll(response.getData());
        }

        Map<Long, AiNutrientVO> nutrientByDietId = nutrients.stream()
                .collect(Collectors.toMap(
                        AiNutrientVO::getUniversalId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
        records.forEach(record -> applyNutrient(record, nutrientByDietId.get(record.getId())));
    }

    private void applyNutrient(DietRecordVO record, AiNutrientVO nutrient) {
        if (nutrient == null) {
            return;
        }
        record.setCalories(parseDecimal(nutrient.getCalory()).intValue());
        record.setProtein(parseDecimal(nutrient.getProtein()));
        record.setFat(parseDecimal(nutrient.getFat()));
        record.setCarbohydrate(parseDecimal(nutrient.getCarbohydrate()));
        record.setSugar(parseDecimal(nutrient.getSugar()));
        record.setSodium(parseDecimal(nutrient.getSodium()));
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        return matcher.find()
                ? new BigDecimal(matcher.group().replace(",", ""))
                : BigDecimal.ZERO;
    }
}
