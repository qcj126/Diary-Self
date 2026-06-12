package diary.common.entity.file.po;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OssUploadSuccessMsg {
    // rabbitMq成功消息体
    private Long id;
    private String ossUrl;
    private String photoName;
    private Long timestamp;
}
