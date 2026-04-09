package diary.utils.validation;

import java.util.regex.Pattern;

/**
 * 中国大陆手机号格式校验（11 位，1 开头）。
 */
public final class PhoneValidationUtil {

    private static final Pattern CN_MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    private PhoneValidationUtil() {
    }

    public static boolean isValid(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String digits = phone.trim();
        return CN_MOBILE.matcher(digits).matches();
    }
}
