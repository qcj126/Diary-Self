package diary.diarylove.impl;

import diary.common.result.ApiResponse;
import diary.diarylove.mapper.DiaryLoveMapper;
import diary.diarylove.service.DiaryLoveDeleteService;
import diary.utils.commonutil.MyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiaryLoveDeleteServiceImpl implements DiaryLoveDeleteService {
    private final DiaryLoveMapper diaryLoveMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteCouple(Long id) {
        return deleteResult(id, diaryLoveMapper::deleteLoveCouple, "删除情侣关系成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteAnniversary(Long id) {
        return deleteResult(id, diaryLoveMapper::deleteLoveAnniversary, "删除纪念日成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteLocation(Long id) {
        return deleteResult(id, diaryLoveMapper::deleteLoveLocation, "删除地点成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteRecord(Long id) {
        /*
         * 当前主记录采用逻辑删除，关联表继续保留，便于后续恢复或审计。
         * TODO 引入 RocketMQ/缓存后，在事务提交后异步重建相册、足迹、年度统计，并同步做最小缓存失效。
         */
        return deleteResult(id, diaryLoveMapper::deleteLoveRecord, "删除恋爱记录成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> deleteRecordImage(Long id) {
        return deleteResult(id, diaryLoveMapper::deleteLoveRecordImage, "删除记录图片成功");
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
