package diary.file.exception;

/** OSS、数据库或输入流等临时故障，RocketMQ可以稍后重投。 */
public class RetryableThumbnailProcessingException extends RuntimeException {
    public RetryableThumbnailProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryableThumbnailProcessingException(String message) {
        super(message);
    }
}
