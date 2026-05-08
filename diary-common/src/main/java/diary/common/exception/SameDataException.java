package diary.common.exception;

public class SameDataException extends RuntimeException {
    public SameDataException(String message) {
        super(message);
    }
}
