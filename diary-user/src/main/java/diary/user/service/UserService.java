package diary.user.service;

import diary.dao.entity.user.dto.UserReqDTO;
import diary.dao.entity.user.po.User;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    Map<String, Object> login(UserReqDTO userDTO);

    Map<String, Object> register(UserReqDTO userDTO);

    Map<String, Object> resetPw(UserReqDTO userDTO);

    Map<String, Object> verifyCode(UserReqDTO userDTO);
}
