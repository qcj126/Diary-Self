package diary.file.enums;

import diary.common.exception.ParamIllegalException;
import lombok.Getter;

@Getter
public enum ImageStatusEnum {
    /*
    *   原状态	            可迁移状态
    *   PENDING_UPLOAD	    UPLOADING、`UPLOAD_FAILED`
    *   UPLOADING	        UPLOADED、`RETRY_WAIT`、`UPLOAD_FAILED`
    *   RETRY_WAIT	        UPLOADING、`UPLOAD_FAILED`
    *   UPLOADED	        READY
    *   READY	            不允许上传流程再次修改
    *   UPLOAD_FAILED	    默认不自动变化；用户主动重试时可重新创建任务
    * */

    READY(1000, "图片可以访问"),
    UPLOAD_FAILED(1100, "上传失败"),
    PENDING_UPLOAD(1200, "等待上传"),
    UPLOADING(1210, "上传处理中"),
    UPLOADED(1220, "云端确认中"),
    RETRY_WAIT(1230, "等待重试");

    private final int code;
    private final String description;

    ImageStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    // 区分职责，只回答true或false
    public static boolean isValid(int code) {
        for (ImageStatusEnum imageStatusEnum : ImageStatusEnum.values()) {
            if (imageStatusEnum.getCode() == code) {
                return true;
            }
        }
        return false;
    }

    // 区分职责，要么返回枚举，要么抛异常
    public static ImageStatusEnum fromCode(int code) {
        for (ImageStatusEnum imageStatusEnum : ImageStatusEnum.values()) {
            if (imageStatusEnum.getCode() == code) {
                return imageStatusEnum;
            }
        }
        throw new ParamIllegalException("未知图片状态编码：" + code);
    }
}
