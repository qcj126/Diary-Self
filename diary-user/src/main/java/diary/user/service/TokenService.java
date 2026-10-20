package diary.user.service;

import diary.user.entity.vo.TokenInfoVO;
import diary.user.entity.vo.TokenPairVO;

import java.util.List;

public interface TokenService {

    TokenPairVO issueTokenPair(Long userId, String username, List<String> roles);

    TokenPairVO refresh(String refreshToken);

    boolean validateAccessToken(String accessToken);

    List<TokenInfoVO> queryTokenInfo(String username);

    void kickOut(String username);
}
