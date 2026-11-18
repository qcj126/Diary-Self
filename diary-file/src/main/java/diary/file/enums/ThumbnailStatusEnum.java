package diary.file.enums;

import diary.common.exception.ParamIllegalException;
import lombok.Getter;

/** 缩略图持久化状态，避免在业务代码中散落0/1/2魔法数字。 */
@Getter
public enum ThumbnailStatusEnum {
    PENDING(0, "等待处理"),
    READY(1, "缩略图可用"),
    FAILED(2, "缩略图处理失败");

    private final int code;
    private final String description;

    ThumbnailStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ThumbnailStatusEnum fromCode(int code) {
        for (ThumbnailStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new ParamIllegalException("未知缩略图状态编码：" + code);
    }
}
