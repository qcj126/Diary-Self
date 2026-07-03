package diary.common.enums.aienum;

import lombok.Getter;

@Getter
public enum AIEnum {
    DEEPSEEK(1, "deepseek"),
    QWEN(2, "通义千问"),
    DBAO(3, "豆包"),
    YBAO(4, "元宝");

    private final int code;
    private final String desc;
    AIEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
