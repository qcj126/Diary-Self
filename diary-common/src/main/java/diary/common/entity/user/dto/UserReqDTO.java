package diary.common.entity.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class UserReqDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;

    private String password;

    private String email;

    private String phone;

    private String code;

    /**
     * Login type: 1 password, 2 verify code.
     */
    private Integer type;

    /**
     * Role codes for admin-created users: admin/user. Defaults to user.
     */
    private List<String> roles;
}
