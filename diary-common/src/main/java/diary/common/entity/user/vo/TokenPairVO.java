package diary.common.entity.user.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class TokenPairVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tokenType;

    private String accessToken;

    private Long accessTokenExpiresIn;

    private String refreshToken;

    private Long refreshTokenExpiresIn;
}
