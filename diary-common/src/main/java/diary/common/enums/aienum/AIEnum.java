package diary.common.enums.aienum;

import lombok.Getter;

@Getter
public enum AIEnum {
    QWEN37PLUS(1, "qwen3.7-plus");

    private final int code;
    private final String desc;
    AIEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
