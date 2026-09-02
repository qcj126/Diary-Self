package diary.diaryinfo.impl.deleteserviceImpl;

import diary.common.result.ApiResponse;
import diary.diaryinfo.mapper.DiaryLoveMapper;
import diary.diaryinfo.service.deleteservice.DiaryLoveDeleteService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiaryLoveDeleteServiceImpl implements DiaryLoveDeleteService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteMood(Long id) {
        return deleteResult(id, diaryLoveMapper::deleteLoveMood, "删除心情成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteTag(Long id) {
        return deleteResult(id, diaryLoveMapper::deleteLoveTag, "删除标签成功");
    }

    private ApiResponse<String> deleteResult(Long id, DeleteOperation operation, String message) {
        MyUtils.check().notNull(id, "id");
        return operation.delete(id) > 0 ? ApiResponse.success(message) : ApiResponse.delFail();
    }

    @FunctionalInterface
    private interface DeleteOperation {
        int delete(Long id);
    }
}
