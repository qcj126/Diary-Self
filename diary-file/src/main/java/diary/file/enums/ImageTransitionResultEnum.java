package diary.file.enums;

import lombok.Getter;

@Getter
public enum ImageTransitionResultEnum {
    APPLIED("本次调用成功完成 CAS"),
    ALREADY_TARGET("其他实例已经迁移到目标状态"),
    REJECTED("当前状态不允许迁移");

    private final String description;

    ImageTransitionResultEnum(String description) {
        this.description = description;
    }
}
