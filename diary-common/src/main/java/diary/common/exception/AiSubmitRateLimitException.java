package diary.common.exception;

public class AiSubmitRateLimitException extends RuntimeException {
    public AiSubmitRateLimitException(String message) {
        super(message);
    }
}
