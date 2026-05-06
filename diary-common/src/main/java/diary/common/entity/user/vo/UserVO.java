package diary.common.entity.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户视图对象
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private LocalDateTime createTime;
}
