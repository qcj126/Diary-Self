package diary.common.exception;

/**
 * 请求与当前资源状态发生冲突。
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}