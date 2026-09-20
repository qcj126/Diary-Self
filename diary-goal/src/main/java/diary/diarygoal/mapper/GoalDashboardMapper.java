package diary.diarygoal.mapper;

import diary.common.entity.goal.po.GoalDashboardCategoryHoursPO;
import diary.common.entity.goal.po.GoalDashboardGoalStatsPO;
import diary.common.entity.goal.po.GoalDashboardStaleGoalPO;
import diary.common.entity.goal.po.GoalDashboardTimeEntryPO;
import diary.common.entity.goal.po.GoalDashboardWorkloadPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface GoalDashboardMapper {
    GoalDashboardGoalStatsPO selectGoalStats(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("cutoffExclusiveDate") LocalDate cutoffExclusiveDate);

    GoalDashboardWorkloadPO selectWorkload(@Param("userId") Long userId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("cutoffDate") LocalDate cutoffDate,
                                           @Param("cutoffExclusiveDate") LocalDate cutoffExclusiveDate);

    List<GoalDashboardTimeEntryPO> selectDailySpentHours(@Param("userId") Long userId,
                                                         @Param("startDate") LocalDate startDate,
                                                         @Param("cutoffDate") LocalDate cutoffDate);

    List<GoalDashboardCategoryHoursPO> selectCategoryHours(@Param("userId") Long userId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("cutoffDate") LocalDate cutoffDate);

    List<GoalDashboardStaleGoalPO> selectStaleGoals(@Param("userId") Long userId,
                                                    @Param("cutoffDate") LocalDate cutoffDate,
                                                    @Param("staleBeforeDate") LocalDate staleBeforeDate);
}
