package diary.dao.entity.timeline.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
