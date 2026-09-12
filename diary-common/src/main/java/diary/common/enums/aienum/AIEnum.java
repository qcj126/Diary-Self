package diary.common.enums.aienum;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AIEnum {
    QWEN37PLUS(1, "qwen3.7-plus"),
    DEEPSEEKV4PRO0813(2, "deepseek-v4-pro-0813");

    private final int code;
    private final String desc;
    AIEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    public static boolean isSupport(Integer code) {
        return Arrays.stream(values()).anyMatch(item -> item.code == code);
    }
}
