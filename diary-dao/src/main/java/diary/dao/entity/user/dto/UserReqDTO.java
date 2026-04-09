package diary.dao.entity.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 */
@Data
public class UserReqDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String code;
    // 1为密码登录，2为验证码登录
    private Integer type;
}
