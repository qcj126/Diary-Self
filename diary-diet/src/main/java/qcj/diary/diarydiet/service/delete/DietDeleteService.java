package qcj.diary.diarydiet.service.delete;

import diary.common.result.ApiResponse;

public interface DietDeleteService {
    ApiResponse<String> deleteDietRecord(Long id);
}
