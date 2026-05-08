package diary.user.service;

import diary.common.entity.user.dto.UserReqDTO;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    Map<String, Object> login(UserReqDTO userDTO);

    String register(UserReqDTO userDTO);

    Map<String, Object> resetPw(UserReqDTO userDTO);

    Map<String, Object> verifyCode(UserReqDTO userDTO);
}
