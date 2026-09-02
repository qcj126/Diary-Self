package diary.diaryinfo.service.deleteservice;

import diary.common.result.ApiResponse;

public interface DiaryLoveDeleteService {
    ApiResponse<String> deleteMood(Long id);

    ApiResponse<String> deleteTag(Long id);
}
