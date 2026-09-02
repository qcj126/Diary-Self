package diary.diarylove.service;

import diary.common.result.ApiResponse;

public interface DiaryLoveDeleteService {
    ApiResponse<String> deleteCouple(Long id);

    ApiResponse<String> deleteAnniversary(Long id);

    ApiResponse<String> deleteLocation(Long id);

    ApiResponse<String> deleteRecord(Long id);

    ApiResponse<String> deleteRecordImage(Long id);
}
