package diary.diarygoal.mapper;

import diary.diarygoal.entity.po.GoalDashboardCategoryHoursPO;
import diary.diarygoal.entity.po.GoalDashboardGoalStatsPO;
import diary.diarygoal.entity.po.GoalDashboardStaleGoalPO;
import diary.diarygoal.entity.po.GoalDashboardTimeEntryPO;
import diary.diarygoal.entity.po.GoalDashboardWorkloadPO;
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
