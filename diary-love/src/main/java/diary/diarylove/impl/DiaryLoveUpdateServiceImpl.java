package diary.diarylove.impl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.entity.love.dto.*;
import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.DiaryLoveUpdateService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DiaryLoveUpdateServiceImpl implements DiaryLoveUpdateService {
    private static final Set<String> RECORD_CATEGORIES = Set.of("DATE", "DAILY", "TRAVEL", "ANNIVERSARY");
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateCouple(LoveCoupleDTO dto) {
        validateCouple(dto);
        if (dto.getStatus() == null) dto.setStatus(1);
        return updateResult(diaryLoveMapper.updateLoveCouple(DtoConvertToPo.convertToPo(dto)), "修改情侣关系成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateAnniversary(LoveAnniversaryDTO dto) {
        validateAnniversary(dto);
        if (dto.getRepeatType() == null) dto.setRepeatType((byte) 1);
        if (dto.getRemindDays() == null) dto.setRemindDays(7);
        if (dto.getPinned() == null) dto.setPinned(false);
        if (dto.getSort() == null) dto.setSort(0);
        return updateResult(diaryLoveMapper.updateLoveAnniversary(DtoConvertToPo.convertToPo(dto)), "修改纪念日成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateLocation(LoveLocationDTO dto) {
        validateLocation(dto);
        return updateResult(diaryLoveMapper.updateLoveLocation(DtoConvertToPo.convertToPo(dto)), "修改地点成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecord(UpdateLoveRecordDto dto) {
        if (dto.getImportant() == null) dto.setImportant(false);
        if (dto.getSort() == null) dto.setSort(0);
        // TODO 接入缓存后，在事务提交成功后删除详情缓存并推进 coupleId 对应的缓存版本；TTL 应增加随机抖动。
        return updateResult(diaryLoveMapper.updateLoveRecord(DtoConvertToPo.convertToPo(dto)), "修改恋爱记录成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecordImage(LoveRecordImageDTO dto) {
        validateRecordImage(dto);
        if (dto.getIsCover() == null) dto.setIsCover(false);
        if (dto.getSort() == null) dto.setSort(0);
        return updateResult(diaryLoveMapper.updateLoveRecordImage(DtoConvertToPo.convertToPo(dto)), "修改记录图片成功");
    }

    private ApiResponse<String> updateResult(int affectedRows, String message) {
        return affectedRows > 0 ? ApiResponse.success(message) : ApiResponse.updateFail();
    }

    void validateLocation(LoveLocationDTO dto) {
        MyUtils.check().notNull(dto, "loveLocationDTO")
                .notNull(dto.getId(), "id")
                .notNull(dto.getCoupleId(), "coupleId").notEmpty(dto.getName(), "name");
        if ((dto.getLongitude() == null) != (dto.getLatitude() == null)) {
            throw new IllegalArgumentException("longitude 和 latitude 必须同时填写或同时为空");
        }
    }

    void validateCouple(LoveCoupleDTO dto) {
        MyUtils.check().notNull(dto, "loveCoupleDTO")
                .notNull(dto.getId(), "id")
                .notNull(dto.getOwnerUserId(), "ownerUserId")
                .notEmpty(dto.getPartnerName(), "partnerName")
                .notEmpty(dto.getStartDate(), "startDate");
    }

    void validateAnniversary(LoveAnniversaryDTO dto) {
        MyUtils.check().notNull(dto, "loveAnniversaryDTO")
                .notNull(dto.getCoupleId(), "coupleId")
                .notNull(dto.getId(), "id")
                .notNull(dto.getCreatorUserId(), "creatorUserId")
                .notEmpty(dto.getName(), "name")
                .notNull(dto.getEventDate(), "eventDate");
    }

    void validateRecordImage(LoveRecordImageDTO dto) {
        MyUtils.check().notNull(dto, "loveRecordImageDTO")
                .notNull(dto.getId(), "id")
                .notNull(dto.getRecordId(), "recordId")
                .notNull(dto.getImageId(), "imageId");
    }
}
