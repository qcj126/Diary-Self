package diary.diaryai.exceptions;

public class AiSubmitRateLimitException extends RuntimeException {
    public AiSubmitRateLimitException(String message) {
        super(message);
    }
}
