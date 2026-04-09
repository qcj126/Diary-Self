package diary.config.result;

/**
 * 注册异常类
 */
public class RegisterException extends RuntimeException {
    
    private final int code;
    
    public RegisterException(String message) {
        super(message);
        this.code = 400;
    }
    
    public RegisterException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}
