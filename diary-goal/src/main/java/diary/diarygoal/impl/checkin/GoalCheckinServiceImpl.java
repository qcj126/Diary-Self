package diary.diarygoal.impl.checkin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.entity.goal.dto.GoalCheckinDTO;
import diary.common.entity.goal.dto.GoalCheckinQueryDTO;
import diary.common.entity.goal.po.GoalCheckinPO;
import diary.common.entity.goal.po.StageGoalPO;
import diary.common.entity.goal.po.SubGoalPO;
import diary.common.entity.goal.vo.GoalCheckinVO;
import diary.common.enums.goalenum.GoalCheckinTypeEnum;
import diary.common.enums.goalenum.GoalStatusEnum;
import diary.common.result.ApiResponse;
import diary.diarygoal.mapper.GoalCheckinMapper;
import diary.diarygoal.service.checkin.GoalCheckinService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GoalCheckinServiceImpl implements GoalCheckinService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal MAX_SPENT_HOURS = new BigDecimal("99999999.99");

    private final GoalCheckinMapper goalCheckinMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<GoalCheckinVO> addCheckin(GoalCheckinDTO request, Long userId) {
        try {
            validateUser(userId);
            normalizeAndValidate(request, true);

            GoalCheckinPO existing = goalCheckinMapper.selectCheckinByRequestId(request.getRequestId(), userId);
            if (existing != null) {
                return ApiResponse.success(toVO(existing));
            }

            StageGoalPO stageGoal = requireStageGoalForUpdate(request.getStageGoalId(), userId);
            SubGoalPO subGoal = requireSubGoalForUpdate(
                    request.getSubGoalId(), request.getStageGoalId(), userId);
            validateAddStatus(stageGoal, subGoal, request.getCheckinType());

            GoalCheckinPO checkin = toPO(request, userId);
            checkin.setId(MyUtils.getPrimaryKey());
            if (goalCheckinMapper.insertCheckin(checkin) != 1) {
                throw new IllegalStateException("打卡记录保存失败");
            }

            adjustLearnedHours(checkin, checkin.getSpentHours());
            refreshStatuses(checkin.getStageGoalId(), checkin.getSubGoalId(), userId);
            GoalCheckinPO saved = goalCheckinMapper.selectCheckinById(checkin.getId(), userId);
            return ApiResponse.success(toVO(saved));
        } catch (DuplicateKeyException exception) {
            GoalCheckinPO existing = request == null || request.getRequestId() == null
                    ? null
                    : goalCheckinMapper.selectCheckinByRequestId(request.getRequestId(), userId);
            if (existing != null) {
                return ApiResponse.success(toVO(existing));
            }
            return ApiResponse.fail(409, "requestId 已被使用");
        } catch (IllegalArgumentException exception) {
            return ApiResponse.fail(400, exception.getMessage());
        } catch (Exception exception) {
            markRollbackOnly();
            return ApiResponse.fail(291, "打卡失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<GoalCheckinVO> updateCheckin(GoalCheckinDTO request, Long userId) {
        try {
            validateUser(userId);
            if (request == null || request.getId() == null) {
                throw new IllegalArgumentException("id 不能为空");
            }

            GoalCheckinPO snapshot = goalCheckinMapper.selectCheckinById(request.getId(), userId);
            if (snapshot == null) {
                return ApiResponse.fail(404, "打卡记录不存在");
            }

            requireStageGoalForUpdate(snapshot.getStageGoalId(), userId);
            GoalCheckinPO existing = goalCheckinMapper.selectCheckinById(request.getId(), userId);
            if (existing == null) {
                return ApiResponse.fail(404, "打卡记录不存在");
            }
            requireSubGoalForUpdate(existing.getSubGoalId(), existing.getStageGoalId(), userId);
            validateImmutableFields(request, existing);

            request.setStageGoalId(existing.getStageGoalId());
            request.setSubGoalId(existing.getSubGoalId());
            request.setRequestId(existing.getRequestId());
            normalizeAndValidate(request, false);

            GoalCheckinPO updated = toPO(request, userId);
            updated.setId(existing.getId());
            updated.setRequestId(existing.getRequestId());
            if (goalCheckinMapper.updateCheckin(updated) != 1) {
                throw new IllegalStateException("打卡记录修改失败");
            }

            BigDecimal hoursDelta = updated.getSpentHours().subtract(existing.getSpentHours());
            adjustLearnedHours(updated, hoursDelta);
            refreshStatuses(updated.getStageGoalId(), updated.getSubGoalId(), userId);
            return ApiResponse.success(toVO(goalCheckinMapper.selectCheckinById(updated.getId(), userId)));
        } catch (IllegalArgumentException exception) {
            return ApiResponse.fail(400, exception.getMessage());
        } catch (Exception exception) {
            markRollbackOnly();
            return ApiResponse.fail(292, "打卡记录修改失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteCheckin(Long id, Long userId) {
        try {
            validateUser(userId);
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }

            GoalCheckinPO snapshot = goalCheckinMapper.selectCheckinById(id, userId);
            if (snapshot == null) {
                return ApiResponse.fail(404, "打卡记录不存在");
            }

            requireStageGoalForUpdate(snapshot.getStageGoalId(), userId);
            GoalCheckinPO existing = goalCheckinMapper.selectCheckinById(id, userId);
            if (existing == null) {
                return ApiResponse.fail(404, "打卡记录不存在");
            }
            requireSubGoalForUpdate(existing.getSubGoalId(), existing.getStageGoalId(), userId);

            if (goalCheckinMapper.softDeleteCheckin(id, userId) != 1) {
                throw new IllegalStateException("打卡记录删除失败");
            }
            adjustLearnedHours(existing, existing.getSpentHours().negate());
            refreshStatuses(existing.getStageGoalId(), existing.getSubGoalId(), userId);
            return ApiResponse.success("打卡记录删除成功");
        } catch (IllegalArgumentException exception) {
            return ApiResponse.fail(400, exception.getMessage());
        } catch (Exception exception) {
            markRollbackOnly();
            return ApiResponse.delFail();
        }
    }

    @Override
    public ApiResponse<GoalCheckinVO> getCheckin(Long id, Long userId) {
        try {
            validateUser(userId);
            if (id == null) {
                throw new IllegalArgumentException("id 不能为空");
            }
            GoalCheckinPO checkin = goalCheckinMapper.selectCheckinById(id, userId);
            if (checkin == null) {
                return ApiResponse.fail(404, "打卡记录不存在");
            }
            return ApiResponse.success(toVO(checkin));
        } catch (IllegalArgumentException exception) {
            return ApiResponse.fail(400, exception.getMessage());
        } catch (Exception exception) {
            return ApiResponse.queryFail();
        }
    }

    @Override
    public ApiResponse<List<GoalCheckinVO>> queryCheckins(GoalCheckinQueryDTO query, Long userId) {
        try {
            validateUser(userId);
            GoalCheckinQueryDTO actualQuery = query == null ? new GoalCheckinQueryDTO() : query;
            normalizeAndValidateQuery(actualQuery);
            int offset = actualQuery.getPageNum() * actualQuery.getPageSize();
            List<GoalCheckinVO> result = goalCheckinMapper.selectCheckins(
                            userId, actualQuery, offset, actualQuery.getPageSize())
                    .stream()
                    .map(this::toVO)
                    .toList();
            return ApiResponse.success(result);
        } catch (IllegalArgumentException exception) {
            return ApiResponse.fail(400, exception.getMessage());
        } catch (Exception exception) {
            return ApiResponse.queryFail();
        }
    }

    private StageGoalPO requireStageGoalForUpdate(Long stageGoalId, Long userId) {
        StageGoalPO stageGoal = goalCheckinMapper.selectStageGoalForUpdate(stageGoalId, userId);
        if (stageGoal == null) {
            throw new IllegalArgumentException("阶段目标不存在或不属于当前用户");
        }
        return stageGoal;
    }

    private SubGoalPO requireSubGoalForUpdate(Long subGoalId, Long stageGoalId, Long userId) {
        if (subGoalId == null) {
            return null;
        }
        SubGoalPO subGoal = goalCheckinMapper.selectSubGoalForUpdate(subGoalId, stageGoalId, userId);
        if (subGoal == null) {
            throw new IllegalArgumentException("子目标不存在、归属错误或不属于当前用户");
        }
        return subGoal;
    }

    private void validateAddStatus(StageGoalPO stageGoal, SubGoalPO subGoal, Integer checkinType) {
        int targetStatus = subGoal == null ? safeStatus(stageGoal.getStatus()) : safeStatus(subGoal.getStatus());
        if (GoalCheckinTypeEnum.REOPEN.getCode().equals(checkinType)) {
            if (targetStatus != GoalStatusEnum.PAUSED.getCode()
                    && targetStatus != GoalStatusEnum.COMPLETED.getCode()
                    && targetStatus != GoalStatusEnum.ABANDONED.getCode()) {
                throw new IllegalArgumentException("只有暂停、已完成或已放弃的目标才能重新开始");
            }
            return;
        }
        if (targetStatus == GoalStatusEnum.COMPLETED.getCode()
                || targetStatus == GoalStatusEnum.ABANDONED.getCode()) {
            throw new IllegalArgumentException("目标已结束，请先重新开始后再打卡");
        }
    }

    private int safeStatus(Integer status) {
        return status == null ? GoalStatusEnum.NOT_STARTED.getCode() : status;
    }

    private void adjustLearnedHours(GoalCheckinPO checkin, BigDecimal hoursDelta) {
        if (checkin.getSubGoalId() == null || hoursDelta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }
        int updated = goalCheckinMapper.adjustSubGoalLearnedHours(
                checkin.getSubGoalId(), checkin.getStageGoalId(), checkin.getUserId(), hoursDelta);
        if (updated != 1) {
            throw new IllegalStateException("子目标学习时长同步失败");
        }
    }

    private void refreshStatuses(Long stageGoalId, Long subGoalId, Long userId) {
        if (subGoalId != null) {
            refreshSubGoalStatus(stageGoalId, subGoalId, userId);
        }
        refreshStageGoalStatus(stageGoalId, userId);
    }

    private void refreshSubGoalStatus(Long stageGoalId, Long subGoalId, Long userId) {
        Integer actionType = goalCheckinMapper.selectLatestSubGoalActionType(subGoalId, stageGoalId, userId);
        int checkinCount = goalCheckinMapper.countSubGoalCheckins(subGoalId, stageGoalId, userId);
        int status;
        if (GoalCheckinTypeEnum.COMPLETE.getCode().equals(actionType)) {
            status = GoalStatusEnum.COMPLETED.getCode();
        } else if (checkinCount > 0) {
            status = GoalStatusEnum.IN_PROGRESS.getCode();
        } else {
            status = GoalStatusEnum.NOT_STARTED.getCode();
        }
        LocalDateTime completedTime = status == GoalStatusEnum.COMPLETED.getCode()
                ? LocalDateTime.now()
                : null;
        if (goalCheckinMapper.updateSubGoalStatus(
                subGoalId, stageGoalId, userId, status, completedTime) != 1) {
            throw new IllegalStateException("子目标状态同步失败");
        }
    }

    private void refreshStageGoalStatus(Long stageGoalId, Long userId) {
        Integer directActionType = goalCheckinMapper.selectLatestStageGoalActionType(stageGoalId, userId);
        int subGoalCount = goalCheckinMapper.countSubGoals(stageGoalId, userId);
        int incompleteSubGoalCount = goalCheckinMapper.countIncompleteSubGoals(stageGoalId, userId);
        int checkinCount = goalCheckinMapper.countStageGoalCheckins(stageGoalId, userId);

        int status;
        if (GoalCheckinTypeEnum.COMPLETE.getCode().equals(directActionType)) {
            status = GoalStatusEnum.COMPLETED.getCode();
        } else if (GoalCheckinTypeEnum.REOPEN.getCode().equals(directActionType)) {
            status = GoalStatusEnum.IN_PROGRESS.getCode();
        } else if (subGoalCount > 0 && incompleteSubGoalCount == 0) {
            status = GoalStatusEnum.COMPLETED.getCode();
        } else if (checkinCount > 0) {
            status = GoalStatusEnum.IN_PROGRESS.getCode();
        } else {
            status = GoalStatusEnum.NOT_STARTED.getCode();
        }

        LocalDateTime completedTime = status == GoalStatusEnum.COMPLETED.getCode()
                ? LocalDateTime.now()
                : null;
        if (goalCheckinMapper.updateStageGoalStatus(stageGoalId, userId, status, completedTime) != 1) {
            throw new IllegalStateException("阶段目标状态同步失败");
        }
    }

    private void normalizeAndValidate(GoalCheckinDTO request, boolean add) {
        MyUtils.Checker checker = MyUtils.check()
                .notNull(request, "checkin");
        if (add) {
            checker.notEmpty(request.getRequestId(), "requestId");
        }
        checker.notNull(request.getStageGoalId(), "stageGoalId");

        if (request.getRequestId() != null && request.getRequestId().length() > 64) {
            throw new IllegalArgumentException("requestId 长度不能超过 64");
        }
        if (request.getCheckinType() == null) {
            request.setCheckinType(GoalCheckinTypeEnum.NORMAL.getCode());
        }
        if (!GoalCheckinTypeEnum.isValid(request.getCheckinType())) {
            throw new IllegalArgumentException("checkinType 只能是 1、2、3、4");
        }
        if (request.getSpentHours() == null) {
            request.setSpentHours(BigDecimal.ZERO);
        }
        if (request.getSpentHours().compareTo(BigDecimal.ZERO) < 0
                || request.getSpentHours().compareTo(MAX_SPENT_HOURS) > 0
                || request.getSpentHours().scale() > 2) {
            throw new IllegalArgumentException("spentHours 必须是 0 到 99999999.99 之间且最多两位小数");
        }
        if (request.getSubGoalId() == null && request.getSpentHours().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("阶段目标打卡不能记录时长，请选择具体子目标");
        }
        if (GoalCheckinTypeEnum.REOPEN.getCode().equals(request.getCheckinType())
                && request.getSpentHours().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("重新开始操作不能记录投入时长");
        }
        if (request.getContent() == null) {
            request.setContent("");
        }
        if (request.getContent().length() > 1000) {
            throw new IllegalArgumentException("content 长度不能超过 1000");
        }
        if (request.getEvidenceUrls() != null) {
            checker.listNotContainsEmpty(request.getEvidenceUrls(), "evidenceUrls");
            if (request.getEvidenceUrls().size() > 20) {
                throw new IllegalArgumentException("evidenceUrls 最多包含 20 个地址");
            }
            boolean invalidUrl = request.getEvidenceUrls().stream()
                    .anyMatch(url -> url.isBlank() || url.length() > 2048);
            if (invalidUrl) {
                throw new IllegalArgumentException("evidenceUrls 包含无效地址");
            }
        }
        if (request.getCheckinDate() == null) {
            request.setCheckinDate(LocalDate.now());
        }
        if (request.getCheckinDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("checkinDate 不能晚于今天");
        }
        if (request.getSource() == null) {
            request.setSource(1);
        }
        if (request.getSource() < 1 || request.getSource() > 4) {
            throw new IllegalArgumentException("source 只能是 1、2、3、4");
        }
    }

    private void validateImmutableFields(GoalCheckinDTO request, GoalCheckinPO existing) {
        if (request.getRequestId() != null && !request.getRequestId().equals(existing.getRequestId())) {
            throw new IllegalArgumentException("requestId 不允许修改");
        }
        if (request.getStageGoalId() != null && !request.getStageGoalId().equals(existing.getStageGoalId())) {
            throw new IllegalArgumentException("stageGoalId 不允许修改，请删除后重新打卡");
        }
        if (!Objects.equals(request.getSubGoalId(), existing.getSubGoalId())
                && request.getSubGoalId() != null) {
            throw new IllegalArgumentException("subGoalId 不允许修改，请删除后重新打卡");
        }
    }

    private void normalizeAndValidateQuery(GoalCheckinQueryDTO query) {
        if (query.getCheckinType() != null && !GoalCheckinTypeEnum.isValid(query.getCheckinType())) {
            throw new IllegalArgumentException("checkinType 只能是 1、2、3、4");
        }
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new IllegalArgumentException("startDate 不能晚于 endDate");
        }
        if (query.getPageNum() == null) {
            query.setPageNum(0);
        }
        if (query.getPageNum() < 0) {
            throw new IllegalArgumentException("pageNum 不能小于 0");
        }
        if (query.getPageSize() == null) {
            query.setPageSize(DEFAULT_PAGE_SIZE);
        }
        if (query.getPageSize() <= 0 || query.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize 必须在 1 到 100 之间");
        }
    }

    private GoalCheckinPO toPO(GoalCheckinDTO request, Long userId) {
        return GoalCheckinPO.builder()
                .requestId(request.getRequestId())
                .userId(userId)
                .stageGoalId(request.getStageGoalId())
                .subGoalId(request.getSubGoalId())
                .checkinType(request.getCheckinType())
                .spentHours(request.getSpentHours())
                .content(request.getContent())
                .evidenceUrls(serializeEvidenceUrls(request.getEvidenceUrls()))
                .checkinDate(request.getCheckinDate())
                .source(request.getSource())
                .deleted(false)
                .build();
    }

    private GoalCheckinVO toVO(GoalCheckinPO checkin) {
        if (checkin == null) {
            return null;
        }
        return GoalCheckinVO.builder()
                .id(checkin.getId())
                .requestId(checkin.getRequestId())
                .userId(checkin.getUserId())
                .stageGoalId(checkin.getStageGoalId())
                .subGoalId(checkin.getSubGoalId())
                .checkinType(checkin.getCheckinType())
                .checkinTypeName(GoalCheckinTypeEnum.getNameByCode(checkin.getCheckinType()))
                .spentHours(checkin.getSpentHours())
                .content(checkin.getContent())
                .evidenceUrls(deserializeEvidenceUrls(checkin.getEvidenceUrls()))
                .checkinDate(checkin.getCheckinDate())
                .source(checkin.getSource())
                .createTime(checkin.getCreateTime())
                .updateTime(checkin.getUpdateTime())
                .build();
    }

    private String serializeEvidenceUrls(List<String> evidenceUrls) {
        if (evidenceUrls == null || evidenceUrls.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(evidenceUrls);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("evidenceUrls 序列化失败", exception);
        }
    }

    private List<String> deserializeEvidenceUrls(String evidenceUrls) {
        if (evidenceUrls == null || evidenceUrls.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(evidenceUrls, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private void validateUser(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用户身份无效");
        }
    }

    private void markRollbackOnly() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
