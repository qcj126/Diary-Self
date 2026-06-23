package diary.common.entity.user.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String username;

    private List<String> roles;

    private String accessTokenId;

    private String refreshTokenId;

    private LocalDateTime accessTokenExpireAt;

    private LocalDateTime refreshTokenExpireAt;
}
