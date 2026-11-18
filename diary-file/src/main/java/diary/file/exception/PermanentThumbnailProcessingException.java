package diary.file.exception;

/** 原图格式、MIME或像素等永久不合法，重试相同对象不会恢复。 */
public class PermanentThumbnailProcessingException extends RuntimeException {
    public PermanentThumbnailProcessingException(String message) {
        super(message);
    }

    public PermanentThumbnailProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
