package diary.common.exception;

/**
 * 同一用户重复使用 clientRequestId，但请求内容与原请求不同。
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
