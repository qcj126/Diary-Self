package diary.diaryai.exceptions;

import diary.common.result.ApiResponse;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.SocketTimeoutException;

/**
 * AI 服务专属异常到 HTTP 响应的映射。
 *
 * <p>上游异常的内部 methodKey/status 只写入日志，响应给客户端的是稳定且不暴露服务内部细节的提示。</p>
 */
@Slf4j
@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(AiSubmitRateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimit(AiSubmitRateLimitException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResponse.fail(HttpStatus.TOO_MANY_REQUESTS.value(), e.getMessage()));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleIdempotencyConflict(IdempotencyConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(HttpStatus.CONFLICT.value(), e.getMessage()));
    }

    /**
     * 关联的食谱、饮食或恋爱记录不存在/不可用时，明确告诉客户端该请求无法被受理。
     */
    @ExceptionHandler(UpstreamBusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleUpstreamBusiness(UpstreamBusinessException exception) {
        log.warn("AI 提交被上游业务拒绝, methodKey={}, status={}",
                exception.getMethodKey(), exception.getUpstreamStatus());
        return failure(HttpStatus.UNPROCESSABLE_ENTITY, "关联业务数据不存在或不可用");
    }

    /**
     * 上游限流或临时不可用时返回 503，调用方可携带原 clientRequestId 稍后重试。
     */
    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUpstreamUnavailable(UpstreamUnavailableException exception) {
        log.warn("AI 上游服务暂不可用, methodKey={}, status={}",
                exception.getMethodKey(), exception.getUpstreamStatus());
        return failure(HttpStatus.SERVICE_UNAVAILABLE, "关联服务暂不可用，请稍后重试");
    }

    /**
     * 上游鉴权、协议或未知服务端错误对当前服务而言属于网关调用失败。
     */
    @ExceptionHandler(UpstreamCallException.class)
    public ResponseEntity<ApiResponse<Void>> handleUpstreamCall(UpstreamCallException exception) {
        log.error("AI 上游服务调用失败, methodKey={}, status={}",
                exception.getMethodKey(), exception.getUpstreamStatus());
        return failure(HttpStatus.BAD_GATEWAY, "关联服务调用失败");
    }

    /**
     * 连接失败或读取超时不会经过 ErrorDecoder；Feign 会以 RetryableException 抛出。
     */
    @ExceptionHandler(RetryableException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignTransport(RetryableException exception) {
        HttpStatus status = hasCause(exception, SocketTimeoutException.class)
                ? HttpStatus.GATEWAY_TIMEOUT
                : HttpStatus.SERVICE_UNAVAILABLE;
        log.warn("AI Feign 传输失败, status={}", status.value(), exception);
        return failure(status, status == HttpStatus.GATEWAY_TIMEOUT
                ? "关联服务响应超时，请稍后重试"
                : "关联服务暂不可用，请稍后重试");
    }

    /**
     * 为未被 ErrorDecoder 分类的 Feign 异常提供兜底，防止直接返回框架默认 500。
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeign(FeignException exception) {
        log.error("AI Feign 调用出现未分类异常, status={}", exception.status(), exception);
        return failure(HttpStatus.BAD_GATEWAY, "关联服务调用失败");
    }

    private ResponseEntity<ApiResponse<Void>> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.fail(status.value(), message));
    }

    /**
     * Feign 会包装底层 I/O 异常；遍历异常链以区分读取超时和连接不可用。
     */
    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (expectedType.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
