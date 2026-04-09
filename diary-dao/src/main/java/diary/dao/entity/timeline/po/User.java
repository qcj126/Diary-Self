package diary.dao.entity.timeline.po;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    private List<String> roles;

    /**
     * 账户是否未过期(1未过期 0已过期)
     */
    private boolean isAccountNonExpired = true;

    /**
     * 账户是否未锁定(1未锁定 0已锁定)
     */
    private boolean isAccountNonLocked = true;

    /**
     * 密码是否未过期(1未过期 0已过期)
     */
    private boolean isCredentialsNonExpired = true;

    /**
     * 账户是否可用(1可用 0不可用)
     */
    private boolean isEnabled = true;
}
