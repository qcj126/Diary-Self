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
    public ResponseEntity<String> handleParamIllegalException(ParamIllegalException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(SameDataException.class)
    public ResponseEntity<String> handleSameDataException(SameDataException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(NullResultException.class)
    public ResponseEntity<String> handleNullResultException(NullResultException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }

    @ExceptionHandler(AiSubmitRateLimitException.class)
    public ResponseEntity<String> handleAiSubmitRateLimitException(AiSubmitRateLimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyConflictException(IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.fail(HttpStatus.CONFLICT.value(), e.getMessage()));
    }
}
