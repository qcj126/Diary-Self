package diary.diarydiet.support;

import diary.diarydiet.entity.dto.DietQueryDTO;
import diary.diarydiet.entity.dto.DietRecordAddDTO;
import diary.diarydiet.entity.dto.DietRecordDTO;
import diary.common.exception.ParamIllegalException;
import diary.utils.common.MyUtils;

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

    public static void validateForAdd(DietRecordAddDTO dto, Long userId) {
        runCheck(() -> MyUtils.check()
                .notNull(dto, "dietRecord")
                .notNull(userId, "userId")
                .notNull(dto.getEatTime(), "eatTime")
                .notNull(dto.getMealType(), "mealType")
                .notEmpty(dto.getFoodName(), "foodName")
                .notEmpty(dto.getLocation(), "location"));
        validateMealType(dto.getMealType());
    }

    public static void validateForUpdate(DietRecordDTO dto) {
        runCheck(() -> MyUtils.check()
                .notNull(dto, "dietRecord")
                .notNull(dto.getId(), "id")
                .notNull(dto.getUserId(), "userId")
                .notNull(dto.getEatTime(), "eatTime")
                .notNull(dto.getMealType(), "mealType")
                .notEmpty(dto.getFoodName(), "foodName")
                .notEmpty(dto.getLocation(), "location"));
        validateMealType(dto.getMealType());
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

    private static void validateMealType(Byte mealType) {
        if (mealType != null && !MEAL_TYPES.contains(mealType)) {
            throw new ParamIllegalException("mealType 不在允许范围内");
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
