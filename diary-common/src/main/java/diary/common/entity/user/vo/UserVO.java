package diary.common.entity.user.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String email;

    private String phone;

    private Integer status;

    private List<String> roles;

    private LocalDateTime createTime;
}
