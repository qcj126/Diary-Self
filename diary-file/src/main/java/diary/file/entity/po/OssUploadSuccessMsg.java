package diary.file.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OssUploadSuccessMsg {
    // rabbitMq成功消息体
    private Long id;
    private String objectKey;    // OSS对象键（文件路径）
    private String photoName;
    private Long timestamp;
}
