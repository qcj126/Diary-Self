package diary.diarygoal.impl.dashboard;

import diary.diarygoal.entity.dto.GoalDashboardQueryDTO;
import diary.diarygoal.entity.po.GoalDashboardCategoryHoursPO;
import diary.diarygoal.entity.po.GoalDashboardGoalStatsPO;
import diary.diarygoal.entity.po.GoalDashboardStaleGoalPO;
import diary.diarygoal.entity.po.GoalDashboardTimeEntryPO;
import diary.diarygoal.entity.po.GoalDashboardWorkloadPO;
import diary.diarygoal.entity.vo.GoalDashboardVO;
import diary.diarygoal.enums.GoalStatusEnum;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalDashboardMapper;
import diary.diarygoal.service.dashboard.GoalDashboardService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoalDashboardServiceImpl implements GoalDashboardService {
    private static final int WEEKLY = 1;
    private static final int MONTHLY = 2;
    private static final int DEFAULT_STALE_DAYS = 7;
    private static final int MAX_STALE_DAYS = 3650;
    private static final int PERCENT_SCALE = 2;
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final GoalDashboardMapper goalDashboardMapper;

    @Override
    public ApiResponse<GoalDashboardVO> getReview(GoalDashboardQueryDTO query, Long userId) {
        try {
            DashboardPeriod period = normalizePeriod(query, userId);
            GoalDashboardGoalStatsPO goalStats = defaultGoalStats(goalDashboardMapper.selectGoalStats(
                    userId, period.startDate(), period.cutoffExclusiveDate()));
            GoalDashboardWorkloadPO workload = defaultWorkload(goalDashboardMapper.selectWorkload(
                    userId, period.startDate(), period.dataThroughDate(), period.cutoffExclusiveDate()));
            List<GoalDashboardTimeEntryPO> dailyEntries = safeList(goalDashboardMapper.selectDailySpentHours(
                    userId, period.startDate(), period.dataThroughDate()));
            List<GoalDashboardCategoryHoursPO> categoryHours = safeList(goalDashboardMapper.selectCategoryHours(
                    userId, period.startDate(), period.dataThroughDate()));
            List<GoalDashboardStaleGoalPO> staleGoals = safeList(goalDashboardMapper.selectStaleGoals(
                    userId, period.dataThroughDate(), period.dataThroughDate().minusDays(period.staleDays())));

            Map<LocalDate, BigDecimal> dailyHours = toDailyHours(dailyEntries);
            BigDecimal periodSpentHours = dailyHours.values().stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Streak streak = calculateStreak(dailyHours, period.startDate(), period.dataThroughDate());

            BigDecimal completedEstimatedHours = zeroIfNull(workload.getCompletedEstimatedHours());
            BigDecimal completedActualHours = zeroIfNull(workload.getCompletedActualHours());
            BigDecimal deviation = completedActualHours.subtract(completedEstimatedHours);
            GoalDashboardVO.Summary summary = GoalDashboardVO.Summary.builder()
                    .dueGoalCount(zeroIfNull(goalStats.getDueGoalCount()))
                    .completedGoalCount(zeroIfNull(goalStats.getCompletedGoalCount()))
                    .overdueGoalCount(zeroIfNull(goalStats.getOverdueGoalCount()))
                    .completionRate(percentage(goalStats.getCompletedGoalCount(), goalStats.getDueGoalCount()))
                    .overdueRate(percentage(goalStats.getOverdueGoalCount(), goalStats.getDueGoalCount()))
                    .periodSpentHours(periodSpentHours)
                    .currentStreakDays(streak.current())
                    .longestStreakDays(streak.longest())
                    .completedEstimatedHours(completedEstimatedHours)
                    .completedActualHours(completedActualHours)
                    .hourDeviation(deviation)
                    .hourDeviationRate(percentage(deviation, completedEstimatedHours))
                    .build();

            return ApiResponse.success(GoalDashboardVO.builder()
                    .periodType(period.periodType())
                    .periodName(period.periodType() == WEEKLY ? "周报" : "月报")
                    .startDate(period.startDate())
                    .endDate(period.endDate())
                    .dataThroughDate(period.dataThroughDate())
                    .summary(summary)
                    .timeTrend(buildTimeTrend(period, dailyHours))
                    .categoryDistribution(buildCategoryDistribution(categoryHours))
                    .burndown(buildBurndown(period, dailyHours, workload))
                    .staleGoals(toStaleGoals(staleGoals))
                    .build());
        } catch (IllegalArgumentException exception) {
            return ApiResponse.fail(400, exception.getMessage());
        } catch (Exception exception) {
            return ApiResponse.queryFail();
        }
    }

    private DashboardPeriod normalizePeriod(GoalDashboardQueryDTO query, Long userId) {
        MyUtils.check().notNull(userId, "userId");
        if (userId <= 0) {
            throw new IllegalArgumentException("用户身份无效");
        }

        GoalDashboardQueryDTO actualQuery = query == null ? new GoalDashboardQueryDTO() : query;
        int periodType = actualQuery.getPeriodType() == null ? WEEKLY : actualQuery.getPeriodType();
        if (periodType != WEEKLY && periodType != MONTHLY) {
            throw new IllegalArgumentException("periodType 只能是 1 或 2");
        }

        LocalDate today = LocalDate.now();
        LocalDate anchorDate = actualQuery.getAnchorDate() == null ? today : actualQuery.getAnchorDate();
        if (anchorDate.isAfter(today)) {
            throw new IllegalArgumentException("anchorDate 不能晚于今天");
        }

        int staleDays = actualQuery.getStaleDays() == null ? DEFAULT_STALE_DAYS : actualQuery.getStaleDays();
        if (staleDays <= 0 || staleDays > MAX_STALE_DAYS) {
            throw new IllegalArgumentException("staleDays 必须在 1 到 3650 之间");
        }

        LocalDate startDate;
        LocalDate endDate;
        if (periodType == WEEKLY) {
            startDate = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endDate = startDate.plusDays(6);
        } else {
            startDate = anchorDate.withDayOfMonth(1);
            endDate = anchorDate.with(TemporalAdjusters.lastDayOfMonth());
        }
        LocalDate dataThroughDate = endDate.isBefore(today) ? endDate : today;
        return new DashboardPeriod(periodType, staleDays, startDate, endDate,
                dataThroughDate, dataThroughDate.plusDays(1));
    }

    private Map<LocalDate, BigDecimal> toDailyHours(List<GoalDashboardTimeEntryPO> dailyEntries) {
        Map<LocalDate, BigDecimal> dailyHours = new HashMap<>();
        for (GoalDashboardTimeEntryPO entry : dailyEntries) {
            if (entry != null && entry.getCheckinDate() != null) {
                dailyHours.merge(entry.getCheckinDate(), zeroIfNull(entry.getSpentHours()), BigDecimal::add);
            }
        }
        return dailyHours;
    }

    private List<GoalDashboardVO.TimeTrendPoint> buildTimeTrend(
            DashboardPeriod period, Map<LocalDate, BigDecimal> dailyHours) {
        List<GoalDashboardVO.TimeTrendPoint> result = new ArrayList<>();
        if (period.periodType() == WEEKLY) {
            LocalDate current = period.startDate();
            while (!current.isAfter(period.dataThroughDate())) {
                result.add(GoalDashboardVO.TimeTrendPoint.builder()
                        .startDate(current)
                        .endDate(current)
                        .label(current.format(DATE_LABEL_FORMATTER))
                        .spentHours(dailyHours.getOrDefault(current, BigDecimal.ZERO))
                        .build());
                current = current.plusDays(1);
            }
            return result;
        }

        LocalDate weekStart = period.startDate();
        while (!weekStart.isAfter(period.dataThroughDate())) {
            LocalDate weekEnd = weekStart.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            if (weekEnd.isAfter(period.dataThroughDate())) {
                weekEnd = period.dataThroughDate();
            }
            BigDecimal spentHours = sumDailyHours(dailyHours, weekStart, weekEnd);
            result.add(GoalDashboardVO.TimeTrendPoint.builder()
                    .startDate(weekStart)
                    .endDate(weekEnd)
                    .label(weekStart.format(DATE_LABEL_FORMATTER) + "~" + weekEnd.format(DATE_LABEL_FORMATTER))
                    .spentHours(spentHours)
                    .build());
            weekStart = weekEnd.plusDays(1);
        }
        return result;
    }

    private List<GoalDashboardVO.CategoryShare> buildCategoryDistribution(
            List<GoalDashboardCategoryHoursPO> categoryHours) {
        BigDecimal total = categoryHours.stream()
                .map(item -> zeroIfNull(item == null ? null : item.getSpentHours()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return categoryHours.stream()
                .filter(item -> item != null && item.getCategory() != null)
                .map(item -> GoalDashboardVO.CategoryShare.builder()
                        .category(item.getCategory())
                        .spentHours(zeroIfNull(item.getSpentHours()))
                        .percentage(percentage(item.getSpentHours(), total))
                        .build())
                .toList();
    }

    private List<GoalDashboardVO.BurndownPoint> buildBurndown(
            DashboardPeriod period,
            Map<LocalDate, BigDecimal> dailyHours,
            GoalDashboardWorkloadPO workload) {
        BigDecimal initialRemaining = zeroIfNull(workload.getTotalEstimatedHours())
                .subtract(zeroIfNull(workload.getInvestedBeforePeriodHours()))
                .max(BigDecimal.ZERO);
        long totalDays = period.startDate().datesUntil(period.endDate().plusDays(1)).count();
        BigDecimal actualRemaining = initialRemaining;
        List<GoalDashboardVO.BurndownPoint> result = new ArrayList<>();
        LocalDate current = period.startDate();
        long elapsedDays = 0;
        while (!current.isAfter(period.dataThroughDate())) {
            elapsedDays++;
            actualRemaining = actualRemaining
                    .subtract(dailyHours.getOrDefault(current, BigDecimal.ZERO))
                    .max(BigDecimal.ZERO);
            BigDecimal idealRemaining = initialRemaining
                    .multiply(BigDecimal.valueOf(totalDays - elapsedDays))
                    .divide(BigDecimal.valueOf(totalDays), PERCENT_SCALE, RoundingMode.HALF_UP);
            result.add(GoalDashboardVO.BurndownPoint.builder()
                    .date(current)
                    .idealRemainingHours(idealRemaining)
                    .actualRemainingHours(actualRemaining)
                    .build());
            current = current.plusDays(1);
        }
        return result;
    }

    private List<GoalDashboardVO.StaleGoal> toStaleGoals(List<GoalDashboardStaleGoalPO> staleGoals) {
        return staleGoals.stream()
                .filter(item -> item != null)
                .map(item -> GoalDashboardVO.StaleGoal.builder()
                        .goalId(item.getGoalId())
                        .title(item.getTitle())
                        .category(item.getCategory())
                        .status(item.getStatus())
                        .statusName(resolveStatusName(item.getStatus()))
                        .lastActivityDate(item.getLastActivityDate())
                        .inactiveDays(zeroIfNull(item.getInactiveDays()))
                        .build())
                .toList();
    }

    private Streak calculateStreak(Map<LocalDate, BigDecimal> dailyHours,
                                   LocalDate startDate,
                                   LocalDate cutoffDate) {
        int currentRun = 0;
        int longestRun = 0;
        LocalDate current = startDate;
        while (!current.isAfter(cutoffDate)) {
            if (dailyHours.containsKey(current)) {
                currentRun++;
                longestRun = Math.max(longestRun, currentRun);
            } else {
                currentRun = 0;
            }
            current = current.plusDays(1);
        }

        int currentStreak = 0;
        current = cutoffDate;
        while (!current.isBefore(startDate) && dailyHours.containsKey(current)) {
            currentStreak++;
            current = current.minusDays(1);
        }
        return new Streak(currentStreak, longestRun);
    }

    private BigDecimal sumDailyHours(Map<LocalDate, BigDecimal> dailyHours,
                                     LocalDate startDate,
                                     LocalDate endDate) {
        BigDecimal result = BigDecimal.ZERO;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            result = result.add(dailyHours.getOrDefault(current, BigDecimal.ZERO));
            current = current.plusDays(1);
        }
        return result;
    }

    private BigDecimal percentage(Number numerator, Number denominator) {
        if (numerator == null || denominator == null) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
        }
        return percentage(new BigDecimal(numerator.toString()), new BigDecimal(denominator.toString()));
    }

    private BigDecimal percentage(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal safeNumerator = zeroIfNull(numerator);
        BigDecimal safeDenominator = zeroIfNull(denominator);
        if (safeDenominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
        }
        return safeNumerator.multiply(BigDecimal.valueOf(100))
                .divide(safeDenominator, PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private String resolveStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return EnumSet.allOf(GoalStatusEnum.class).stream()
                .filter(item -> item.getCode().equals(status))
                .map(GoalStatusEnum::getName)
                .findFirst()
                .orElse("未知");
    }

    private GoalDashboardGoalStatsPO defaultGoalStats(GoalDashboardGoalStatsPO goalStats) {
        return goalStats == null ? new GoalDashboardGoalStatsPO() : goalStats;
    }

    private GoalDashboardWorkloadPO defaultWorkload(GoalDashboardWorkloadPO workload) {
        return workload == null ? new GoalDashboardWorkloadPO() : workload;
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }

    private record DashboardPeriod(Integer periodType,
                                   Integer staleDays,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   LocalDate dataThroughDate,
                                   LocalDate cutoffExclusiveDate) {
    }

    private record Streak(Integer current, Integer longest) {
    }
}
