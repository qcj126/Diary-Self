package diary.file.exception;

/**
 * OSS Complete 已生成对象，但对象大小与业务会话期望不一致。
 */
public class OssObjectSizeMismatchException extends IllegalStateException {

    public OssObjectSizeMismatchException(long expectedSize, long actualSize) {
        super("OSS合并后的对象大小不一致，expected=" + expectedSize + "，actual=" + actualSize);
    }
}
