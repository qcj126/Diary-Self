package diary.common.entity.image.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageVO {
    private Long id;
    private String url;
}
