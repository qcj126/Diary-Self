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
        validateMood(dto);
        dto.setId(MyUtils.getPrimaryKey());
        if (dto.getSort() == null) dto.setSort(0);
        if (dto.getEnabled() == null) dto.setEnabled(true);
        return addResult(diaryLoveMapper.insertLoveMood(DtoConvertToPo.convertToPo(dto)), "添加心情成功");
    }

    void validateMood(LoveMoodDTO dto) {
        MyUtils.check().notNull(dto, "loveMoodDTO")
                .notNull(dto.getId(), "id")
                .notEmpty(dto.getMoodName(), "moodName");
    }

    private ApiResponse<String> addResult(int affectedRows, String message) {
        return affectedRows > 0 ? ApiResponse.success(message) : ApiResponse.addFail();
    }
}
