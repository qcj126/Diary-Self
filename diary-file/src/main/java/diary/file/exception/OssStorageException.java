package diary.file.exception;

public class OssStorageException extends RuntimeException {

    public OssStorageException(String operation, String objectKey, Throwable cause) {
        super("OSS操作失败，operation=" + operation + "，objectKey=" + objectKey, cause);
    }
}
