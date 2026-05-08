package diary.common.exception;

public class CustomException extends RuntimeException {
    /**
     * 通用异常处理器，用于处理操作失败的场景
     * @param message
     */
    public CustomException(String message) {
        super(message);
    }
}
