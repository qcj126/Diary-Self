package diary.common.enums.aienum;

import lombok.Getter;

@Getter
public enum AIEnum {
    DEEPSEEK(1, "deepseek"),
    QWENMAX(2, "通义千问-MAX"),
    QWENPLUS(3, "通义千问-PLUS"),
    YBAO(4, "元宝");

    private final int code;
    private final String desc;
    AIEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
