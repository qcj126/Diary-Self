package diary.diaryinfo.impl.addserviceImpl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.entity.love.dto.*;
import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.DiaryLoveMapper;
import diary.diaryinfo.service.addservice.DiaryLoveAddService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryLoveAddServiceImpl implements DiaryLoveAddService {

    private final DiaryLoveMapper diaryLoveMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> addLocation(LoveLocationDTO dto) {
        validateLocation(dto, false);
        dto.setId(MyUtils.getPrimaryKey());
        return addResult(diaryLoveMapper.insertLoveLocation(DtoConvertToPo.convertToPo(dto)), "添加地点成功");
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
