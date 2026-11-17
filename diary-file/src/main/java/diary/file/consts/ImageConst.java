package diary.file.consts;

import java.util.Set;
import java.util.regex.Pattern;

public class ImageConst {
    public static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-f]{64}$");
    public static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    public static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    public static final String PNG_MIME_TYPE = "image/png";
    public static final String JPEG_MIME_TYPE = "image/jpeg";
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg");

    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");
}
