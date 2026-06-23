package diary.user.service;

import diary.common.entity.user.vo.TokenInfoVO;
import diary.common.entity.user.vo.TokenPairVO;

import java.util.List;

public interface TokenService {

    TokenPairVO issueTokenPair(String username, List<String> roles);

    TokenPairVO refresh(String refreshToken);

    boolean validateAccessToken(String accessToken);

    List<TokenInfoVO> queryTokenInfo(String username);

    void kickOut(String username);
}
