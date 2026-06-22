package diary.user.service;

import diary.common.entity.user.dto.UserReqDTO;
import diary.common.entity.user.vo.TokenPairVO;
import diary.common.entity.user.vo.UserVO;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    Map<String, Object> login(UserReqDTO userDTO);

    String register(UserReqDTO userDTO);

    String addUser(UserReqDTO userDTO);

    String deleteUser(String username);

    List<UserVO> queryUsers();

    Map<String, Object> resetPw(UserReqDTO userDTO);

    Map<String, Object> verifyCode(UserReqDTO userDTO);

    TokenPairVO refresh(String refreshToken);
}
