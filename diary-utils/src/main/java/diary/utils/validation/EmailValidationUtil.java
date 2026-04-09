package diary.utils.validation;

import java.util.regex.Pattern;

/**
 * 邮箱格式校验工具。
 */
public final class EmailValidationUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

    private EmailValidationUtil() {
    }

    public static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL.matcher(email.trim()).matches();
    }
}
