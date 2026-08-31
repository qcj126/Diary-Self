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
import diary.diarylove.service.DiaryLoveUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryLoveUpdateServiceImpl implements DiaryLoveUpdateService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateCouple(LoveCoupleDTO dto) {
        DiaryLoveAddServiceImpl.validateCouple(dto, true);
        if (dto.getStatus() == null) dto.setStatus(1);
        return updateResult(diaryLoveMapper.updateLoveCouple(DtoConvertToPo.convertToPo(dto)), "修改情侣关系成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateAnniversary(LoveAnniversaryDTO dto) {
        DiaryLoveAddServiceImpl.validateAnniversary(dto, true);
        if (dto.getRepeatType() == null) dto.setRepeatType((byte) 1);
        if (dto.getRemindDays() == null) dto.setRemindDays(7);
        if (dto.getPinned() == null) dto.setPinned(false);
        if (dto.getSort() == null) dto.setSort(0);
        return updateResult(diaryLoveMapper.updateLoveAnniversary(DtoConvertToPo.convertToPo(dto)), "修改纪念日成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateLocation(LoveLocationDTO dto) {
        DiaryLoveAddServiceImpl.validateLocation(dto, true);
        return updateResult(diaryLoveMapper.updateLoveLocation(DtoConvertToPo.convertToPo(dto)), "修改地点成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecord(LoveRecordDTO dto) {
        DiaryLoveAddServiceImpl.validateRecord(dto, true);
        if (dto.getImportant() == null) dto.setImportant(false);
        if (dto.getSort() == null) dto.setSort(0);

        // TODO 接入缓存后，在事务提交成功后删除详情缓存并推进 coupleId 对应的缓存版本；TTL 应增加随机抖动。
        return updateResult(diaryLoveMapper.updateLoveRecord(DtoConvertToPo.convertToPo(dto)), "修改恋爱记录成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecordImage(LoveRecordImageDTO dto) {
        DiaryLoveAddServiceImpl.validateRecordImage(dto, true);
        if (dto.getIsCover() == null) dto.setIsCover(false);
        if (dto.getSort() == null) dto.setSort(0);
        return updateResult(diaryLoveMapper.updateLoveRecordImage(DtoConvertToPo.convertToPo(dto)), "修改记录图片成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateMood(LoveMoodDTO dto) {
        DiaryLoveAddServiceImpl.validateMood(dto, true);
        if (dto.getSort() == null) dto.setSort(0);
        if (dto.getEnabled() == null) dto.setEnabled(true);
        return updateResult(diaryLoveMapper.updateLoveMood(DtoConvertToPo.convertToPo(dto)), "修改心情成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecordMood(LoveRecordMoodDTO dto) {
        DiaryLoveAddServiceImpl.validateRecordMood(dto, true);
        if (dto.getSort() == null) dto.setSort(0);
        return updateResult(diaryLoveMapper.updateLoveRecordMood(DtoConvertToPo.convertToPo(dto)), "修改记录心情成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateTag(LoveTagDTO dto) {
        DiaryLoveAddServiceImpl.validateTag(dto, true);
        if (dto.getUseCount() == null) dto.setUseCount(0);
        return updateResult(diaryLoveMapper.updateLoveTag(DtoConvertToPo.convertToPo(dto)), "修改标签成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateRecordTag(LoveRecordTagDTO dto) {
        DiaryLoveAddServiceImpl.validateRecordTag(dto, true);
        if (dto.getSort() == null) dto.setSort(0);
        return updateResult(diaryLoveMapper.updateLoveRecordTag(DtoConvertToPo.convertToPo(dto)), "修改记录标签成功");
    }

    private ApiResponse<String> updateResult(int affectedRows, String message) {
        return affectedRows > 0 ? ApiResponse.success(message) : ApiResponse.updateFail();
    }
}
