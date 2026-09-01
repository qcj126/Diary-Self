package diary.diarydiet.support;

import diary.common.entity.diet.dto.DietQueryDTO;
import diary.common.entity.diet.dto.DietRecordDTO;
import diary.common.exception.ParamIllegalException;
import diary.utils.commonutil.MyUtils;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 饮食模块统一入参校验入口。
 */
public final class DietRecordValidator {
    private static final Set<Byte> MEAL_TYPES = Set.of(
            (byte) 10, (byte) 15, (byte) 20, (byte) 25, (byte) 30, (byte) 35
    );

    private DietRecordValidator() {
    }

    public static void validateForAdd(DietRecordDTO dto) {
        runCheck(() -> MyUtils.check()
                .notNull(dto, "dietRecord")
                .notNull(dto.getUserId(), "userId")
                .notNull(dto.getEatTime(), "eatTime")
                .notNull(dto.getMealType(), "mealType")
                .notEmpty(dto.getFoodName(), "foodName")
                .notNull(dto.getCalories(), "calories")
                .notNull(dto.getProtein(), "protein")
                .notNull(dto.getFat(), "fat")
                .notNull(dto.getCarbohydrate(), "carbohydrate")
                .notNull(dto.getSugar(), "sugar")
                .notNull(dto.getSodium(), "sodium")
                .notEmpty(dto.getLocation(), "location"));
        validateValues(dto);
    }

    public static void validateForUpdate(DietRecordDTO dto) {
        runCheck(() -> MyUtils.check().notNull(dto, "dietRecord").notNull(dto.getId(), "id"));
        validateForAdd(dto);
    }

    public static void validateQuery(DietQueryDTO query) {
        runCheck(() -> MyUtils.check().notNull(query, "query").notNull(query.getUserId(), "userId"));
        if (query.getStartTime() != null && query.getEndTime() != null
                && query.getStartTime().isAfter(query.getEndTime())) {
            throw new ParamIllegalException("startTime 不能晚于 endTime");
        }
        validateMealType(query.getMealType());
    }

    public static void validateId(Long id) {
        runCheck(() -> MyUtils.check().notNull(id, "id"));
    }

    private static void validateValues(DietRecordDTO dto) {
        validateMealType(dto.getMealType());
        if (dto.getCalories() < 0) {
            throw new ParamIllegalException("calories 不能为负数");
        }
        validateNonNegative(dto.getProtein(), "protein");
        validateNonNegative(dto.getFat(), "fat");
        validateNonNegative(dto.getCarbohydrate(), "carbohydrate");
        validateNonNegative(dto.getSugar(), "sugar");
        validateNonNegative(dto.getSodium(), "sodium");
        if (dto.getFullnessScore() != null
                && (dto.getFullnessScore() < 1 || dto.getFullnessScore() > 10)) {
            throw new ParamIllegalException("fullnessScore 必须在 1 到 10 之间");
        }
    }

    private static void validateMealType(Byte mealType) {
        if (mealType != null && !MEAL_TYPES.contains(mealType)) {
            throw new ParamIllegalException("mealType 不在允许范围内");
        }
    }

    private static void validateNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ParamIllegalException(fieldName + " 不能为负数");
        }
    }

    private static void runCheck(Runnable validation) {
        try {
            validation.run();
        } catch (IllegalArgumentException exception) {
            throw new ParamIllegalException(exception.getMessage());
        }
    }
}
