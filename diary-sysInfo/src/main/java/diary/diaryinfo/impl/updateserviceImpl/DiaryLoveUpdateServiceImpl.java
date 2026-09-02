package diary.diaryinfo.impl.updateserviceImpl;

import diary.common.convert.love.DtoConvertToPo;
import diary.common.entity.love.dto.LoveMoodDTO;
import diary.common.entity.love.dto.LoveTagDTO;
import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.DiaryLoveMapper;
import diary.diaryinfo.service.updateservice.DiaryLoveUpdateService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryLoveUpdateServiceImpl implements DiaryLoveUpdateService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateMood(LoveMoodDTO dto) {
        validateMood(dto);
        if (dto.getSort() == null) dto.setSort(0);
        if (dto.getEnabled() == null) dto.setEnabled(true);
        return updateResult(diaryLoveMapper.updateLoveMood(DtoConvertToPo.convertToPo(dto)), "修改心情成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> updateTag(LoveTagDTO dto) {
        validateTag(dto);
        if (dto.getUseCount() == null) dto.setUseCount(0);
        return updateResult(diaryLoveMapper.updateLoveTag(DtoConvertToPo.convertToPo(dto)), "修改标签成功");
    }

    void validateMood(LoveMoodDTO dto) {
        MyUtils.check().notNull(dto, "loveMoodDTO")
            .notNull(dto.getId(), "id")
            .notEmpty(dto.getMoodCode(), "moodCode")
            .notEmpty(dto.getMoodName(), "moodName");
    }
    void validateTag(LoveTagDTO dto) {
        MyUtils.check().notNull(dto, "loveTagDTO")
            .notNull(dto.getId(), "id")
            .notNull(dto.getCoupleId(), "coupleId")
            .notNull(dto.getCreatorUserId(), "creatorUserId")
            .notEmpty(dto.getTagName(), "tagName");
    }
    private ApiResponse<String> updateResult(int affectedRows, String message) {
        return affectedRows > 0 ? ApiResponse.success(message) : ApiResponse.updateFail();
    }
}
