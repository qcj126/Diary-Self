package diary.diarylove.impl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.entity.love.dto.LoveAnniversaryDTO;
import diary.common.entity.love.dto.LoveCoupleDTO;
import diary.common.entity.love.dto.LoveLocationDTO;
import diary.common.entity.love.dto.LoveMoodDTO;
import diary.common.entity.love.dto.LoveRecordDTO;
import diary.common.entity.love.dto.LoveRecordImageDTO;
import diary.common.entity.love.dto.LoveRecordMoodDTO;
import diary.common.entity.love.dto.LoveRecordTagDTO;
import diary.common.entity.love.dto.LoveTagDTO;
import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.DiaryLoveAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiaryLoveAddServiceImpl implements DiaryLoveAddService {
    private static final Set<String> RECORD_CATEGORIES = Set.of("DATE", "DAILY", "TRAVEL", "ANNIVERSARY");

    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addCouples(LoveCoupleDTO dto) {
        validateCouple(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getStatus() == null) dto.setStatus(1);
        return addResult(diaryLoveMapper.insertLoveCouple(DtoConvertToPo.convertToPo(dto)), "添加情侣关系成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addAnniversary(LoveAnniversaryDTO dto) {
        validateAnniversary(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getRepeatType() == null) dto.setRepeatType((byte) 1);
        if (dto.getRemindDays() == null) dto.setRemindDays(7);
        if (dto.getPinned() == null) dto.setPinned(false);
        if (dto.getSort() == null) dto.setSort(0);
        return addResult(diaryLoveMapper.insertLoveAnniversary(DtoConvertToPo.convertToPo(dto)), "添加纪念日成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addLocation(LoveLocationDTO dto) {
        validateLocation(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        return addResult(diaryLoveMapper.insertLoveLocation(DtoConvertToPo.convertToPo(dto)), "添加地点成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addRecord(LoveRecordDTO dto) {
        validateRecord(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getImportant() == null) dto.setImportant(false);
        if (dto.getSort() == null) dto.setSort(0);

        /*
         * TODO 后续将记录、图片、心情、标签和地点关联合并为一个事务用例，并在同一事务写 Outbox。
         *      RocketMQ 只负责提交后的异步统计/缓存投影，不能成为基础新增成功的前置条件。
         */
        return addResult(diaryLoveMapper.insertLoveRecord(DtoConvertToPo.convertToPo(dto)), "添加恋爱记录成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addRecordImage(LoveRecordImageDTO dto) {
        validateRecordImage(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getIsCover() == null) dto.setIsCover(false);
        if (dto.getSort() == null) dto.setSort(0);
        return addResult(diaryLoveMapper.insertLoveRecordImage(DtoConvertToPo.convertToPo(dto)), "添加记录图片成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addMood(LoveMoodDTO dto) {
        validateMood(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getSort() == null) dto.setSort(0);
        if (dto.getEnabled() == null) dto.setEnabled(true);
        return addResult(diaryLoveMapper.insertLoveMood(DtoConvertToPo.convertToPo(dto)), "添加心情成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addRecordMood(LoveRecordMoodDTO dto) {
        validateRecordMood(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getSort() == null) dto.setSort(0);
        return addResult(diaryLoveMapper.insertLoveRecordMood(DtoConvertToPo.convertToPo(dto)), "添加记录心情成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addTag(LoveTagDTO dto) {
        validateTag(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getUseCount() == null) dto.setUseCount(0);
        return addResult(diaryLoveMapper.insertLoveTag(DtoConvertToPo.convertToPo(dto)), "添加标签成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addRecordTag(LoveRecordTagDTO dto) {
        validateRecordTag(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getSort() == null) dto.setSort(0);
        return addResult(diaryLoveMapper.insertLoveRecordTag(DtoConvertToPo.convertToPo(dto)), "添加记录标签成功");
    }

    static void validateCouple(LoveCoupleDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveCoupleDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getOwnerUserId(), "ownerUserId")
                .notEmpty(dto.getPartnerName(), "partnerName")
                .notEmpty(dto.getStartDate(), "startDate");
    }

    static void validateAnniversary(LoveAnniversaryDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveAnniversaryDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId")
                .notNull(dto.getCreatorUserId(), "creatorUserId")
                .notEmpty(dto.getName(), "name")
                .notNull(dto.getEventDate(), "eventDate");
    }

    static void validateLocation(LoveLocationDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveLocationDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId").notEmpty(dto.getName(), "name");
        if ((dto.getLongitude() == null) != (dto.getLatitude() == null)) {
            throw new IllegalArgumentException("longitude 和 latitude 必须同时填写或同时为空");
        }
    }

    static void validateRecord(LoveRecordDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId")
                .notNull(dto.getCreatorUserId(), "creatorUserId")
                .notEmpty(dto.getTitle(), "title")
                .notNull(dto.getRecordDate(), "recordDate")
                .notEmpty(dto.getCategoryCode(), "categoryCode");
        if (!RECORD_CATEGORIES.contains(dto.getCategoryCode())) {
            throw new IllegalArgumentException("categoryCode 只能是 DATE、DAILY、TRAVEL 或 ANNIVERSARY");
        }
    }

    static void validateRecordImage(LoveRecordImageDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordImageDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getRecordId(), "recordId").notNull(dto.getImageId(), "imageId");
    }

    static void validateMood(LoveMoodDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveMoodDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notEmpty(dto.getMoodCode(), "moodCode").notEmpty(dto.getMoodName(), "moodName");
    }

    static void validateRecordMood(LoveRecordMoodDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordMoodDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getRecordId(), "recordId").notNull(dto.getMoodId(), "moodId");
    }

    static void validateTag(LoveTagDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveTagDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getCoupleId(), "coupleId")
                .notNull(dto.getCreatorUserId(), "creatorUserId")
                .notEmpty(dto.getTagName(), "tagName");
    }

    static void validateRecordTag(LoveRecordTagDTO dto, boolean requireId) {
        MyUtils.Checker checker = MyUtils.check().notNull(dto, "loveRecordTagDTO");
        if (requireId) checker.notNull(dto.getId(), "id");
        checker.notNull(dto.getRecordId(), "recordId").notNull(dto.getTagId(), "tagId");
    }

    private ApiResponse<String> addResult(int affectedRows, String message) {
        return affectedRows > 0 ? ApiResponse.success(message) : ApiResponse.addFail();
    }
}
