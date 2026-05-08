package diary.common.exception;

import diary.common.result.ApiResponse;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse handleParamIllegalException(ParamIllegalException e) {
        return ApiResponse.fail(500, e.getMessage());
    }

    @ExceptionHandler(SameDataException.class)
    public ApiResponse handleSameDataException(SameDataException e) {
        return ApiResponse.fail(500, e.getMessage());
    }
}
