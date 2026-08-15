package diary.common.exception;

import diary.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 处理参数非法异常
     */
    @ExceptionHandler(ParamIllegalException.class)
    public ApiResponse<Void> handleParamIllegalException(ParamIllegalException e) {
        return ApiResponse.fail(500, e.getMessage());
    }

    @ExceptionHandler(SameDataException.class)
    public ApiResponse<Void> handleSameDataException(SameDataException e) {
        return ApiResponse.fail(500, e.getMessage());
    }

    @ExceptionHandler(CustomException.class)
    public ApiResponse<Void> handleCustomException(CustomException e) {
        return ApiResponse.fail(500, e.getMessage());
    }

    @ExceptionHandler(NullResultException.class)
    public ApiResponse<Void> handleNullResultException(NullResultException e) {
        return ApiResponse.fail(500, e.getMessage());
    }

    @ExceptionHandler(AiSubmitRateLimitException.class)
    public ResponseEntity<String> handleAiSubmitRateLimitException(AiSubmitRateLimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
    }
}
