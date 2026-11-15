package diary.file.enums;

import diary.common.exception.ParamIllegalException;
import lombok.Getter;

/**
 * 图片分片上传会话状态
 * 与 image_upload_session.status 的 TINYINT 取值一一对应
 */
@Getter
public enum ImageUploadSessionStatusEnum {

    /** 0-初始化 */
    INITIATING(0, "初始化"),

    /** 1-上传中 */
    UPLOADING(1, "上传中"),

    /** 2-合并中 */
    COMPLETING(2, "合并中"),

    /** 3-完成 */
    COMPLETED(3, "完成"),

    /** 4-已中止 */
    ABORTED(4, "已中止"),

    /** 5-过期 */
    EXPIRED(5, "过期");

    private final int code;
    private final String desc;

    ImageUploadSessionStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 按 code 反查，拒绝空值和未知状态。 */
    public static ImageUploadSessionStatusEnum fromCode(Integer code) {
        if (code != null) {
            for (ImageUploadSessionStatusEnum status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        throw new ParamIllegalException("未知上传会话状态编码：" + code);
    }
}
