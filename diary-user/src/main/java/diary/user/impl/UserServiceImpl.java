package diary.user.impl;

import diary.common.entity.user.dto.UserReqDTO;
import diary.common.entity.user.po.User;
import diary.common.entity.user.vo.TokenPairVO;
import diary.common.entity.user.vo.UserVO;
import diary.common.exception.CustomException;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.user.mapper.UserMapper;
import diary.user.service.TokenService;
import diary.user.service.UserService;
import diary.utils.commonutil.MyUtils;
import diary.utils.redis.ValidatUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static diary.utils.crypto.BCryptUtilNoSpring.encode;
import static diary.utils.crypto.BCryptUtilNoSpring.matches;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private ValidatUtil validatUtil;

    @Resource
    private TokenService tokenService;

    @Override
    public Map<String, Object> login(UserReqDTO userDTO) {
        if (userDTO.getType() == null) {
            throw new ParamIllegalException("login type is required");
        }
        if (userDTO.getType() != 1 && userDTO.getType() != 2) {
            throw new ParamIllegalException("unsupported login type");
        }

        User user = userDTO.getType() == 2 ? loginByCode(userDTO) : loginByPassword(userDTO);
        List<String> roles = loadRoleCodes(user.getUserId());
        TokenPairVO tokenPair = tokenService.issueTokenPair(user.getUsername(), roles);

        // OAuth2-style Bearer tokens: access token for APIs, refresh token for renewal.
        return Map.of(
                "tokenType", tokenPair.getTokenType(),
                "accessToken", tokenPair.getAccessToken(),
                "accessTokenExpiresIn", tokenPair.getAccessTokenExpiresIn(),
                "refreshToken", tokenPair.getRefreshToken(),
                "refreshTokenExpiresIn", tokenPair.getRefreshTokenExpiresIn()
        );
    }

    private User loginByPassword(UserReqDTO userDTO) {
        if (isBlank(userDTO.getUsername()) && isBlank(userDTO.getEmail()) && isBlank(userDTO.getPhone())) {
            throw new ParamIllegalException("username, email or phone is required");
        }
        if (isBlank(userDTO.getPassword())) {
            throw new ParamIllegalException("password is required");
        }

        User user = loadUserByIdentify(userDTO);
        if (!matches(userDTO.getPassword(), user.getPassword())) {
            throw new CustomException("password is incorrect");
        }
        return user;
    }

    private User loginByCode(UserReqDTO userDTO) {
        if (isBlank(userDTO.getPhone())) {
            throw new ParamIllegalException("phone is required");
        }
        if (isBlank(userDTO.getCode())) {
            throw new ParamIllegalException("verify code is required");
        }
        if (!validatUtil.checkVerifyCode(userDTO.getCode())) {
            throw new CustomException("verify code is invalid or expired");
        }
        return loadUserByUsername(userDTO.getPhone());
    }

    private User loadUserByIdentify(UserReqDTO userDTO) {
        if (!isBlank(userDTO.getEmail())) {
            return loadUserByUsername(userDTO.getEmail());
        }
        if (!isBlank(userDTO.getUsername())) {
            return loadUserByUsername(userDTO.getUsername());
        }
        if (!isBlank(userDTO.getPhone())) {
            return loadUserByUsername(userDTO.getPhone());
        }
        throw new ParamIllegalException("user identify is required");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(UserReqDTO userDTO) {
        createUser(userDTO, List.of("user"));
        return "register success";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addUser(UserReqDTO userDTO) {
        createUser(userDTO, normalizeRoles(userDTO.getRoles()));
        return "add user success";
    }

    @Override
    public String deleteUser(String username) {
        if (isBlank(username)) {
            throw new ParamIllegalException("username is required");
        }
        int affected = userMapper.disableUserByUsername(username);
        tokenService.kickOut(username);
        if (affected <= 0) {
            throw new CustomException("delete user failed");
        }
        return "delete success";
    }

    @Override
    public List<UserVO> queryUsers() {
        return userMapper.selectUsers().stream().map(this::toUserVO).toList();
    }

    private void createUser(UserReqDTO userDTO, List<String> roles) {
        validateCreateUser(userDTO);
        assertUniqueUser(userDTO);

        User newUser = new User();
        newUser.setUserId(MyUtils.getPrimaryKey());
        newUser.setUsername(userDTO.getUsername());
        newUser.setEmail(userDTO.getEmail());
        newUser.setPhone(userDTO.getPhone());
        newUser.setPassword(encode(userDTO.getPassword()));

        int result = userMapper.userRegister(newUser);
        if (result <= 0) {
            throw new CustomException("save user failed");
        }

        // user_role stores the user-role relation. Register defaults to user.
        for (String role : roles) {
            Long roleId = userMapper.selectRoleIdByCode(role);
            if (roleId == null) {
                throw new ParamIllegalException("role does not exist: " + role);
            }
            userMapper.insertUserRole(MyUtils.getPrimaryKey(), newUser.getUserId(), roleId);
        }
        log.info("user saved: {}", userDTO.getUsername());
    }

    private void validateCreateUser(UserReqDTO userDTO) {
        if (isBlank(userDTO.getUsername())) {
            throw new ParamIllegalException("username is required");
        }
        if (isBlank(userDTO.getEmail())) {
            throw new ParamIllegalException("email is required");
        }
        if (isBlank(userDTO.getPhone())) {
            throw new ParamIllegalException("phone is required");
        }
        if (isBlank(userDTO.getPassword())) {
            throw new ParamIllegalException("password is required");
        }
    }

    private void assertUniqueUser(UserReqDTO userDTO) {
        if (userMapper.selectByUsername(userDTO.getUsername()) != null) {
            throw new SameDataException("username already exists");
        }
        if (userMapper.selectByEmail(userDTO.getEmail()) != null) {
            throw new SameDataException("email already exists");
        }
        if (userMapper.selectByPhone(userDTO.getPhone()) != null) {
            throw new SameDataException("phone already exists");
        }
    }

    @Override
    public Map<String, Object> resetPw(UserReqDTO userDTO) {
        if (isBlank(userDTO.getPassword())) {
            throw new ParamIllegalException("new password is required");
        }
        User user = loadUserByIdentify(userDTO);
        userMapper.updatePassword(user.getUsername(), encode(userDTO.getPassword()));
        tokenService.kickOut(user.getUsername());
        return Map.of("message", "reset password success");
    }

    @Override
    public Map<String, Object> verifyCode(UserReqDTO userDTO) {
        if (isBlank(userDTO.getPhone())) {
            throw new ParamIllegalException("phone is required");
        }
        loadUserByUsername(userDTO.getPhone());
        String code = String.valueOf(Math.random()).substring(2, 8);
        validatUtil.setVerifyCode(code);
        return Map.of("message", "verify code sent");
    }

    @Override
    public TokenPairVO refresh(String refreshToken) {
        return tokenService.refresh(refreshToken);
    }

    public User loadUserByUsername(String identify) {
        User user;
        if (identify.contains("@")) {
            user = userMapper.selectByEmail(identify);
        } else if (identify.matches("^1[3-9]\\d{9}$")) {
            user = userMapper.selectByPhone(identify);
        } else {
            user = userMapper.selectByUsername(identify);
        }
        if (user == null) {
            throw new CustomException("user does not exist");
        }
        user.setRoles(loadRoleCodes(user.getUserId()));
        return user;
    }

    private List<String> loadRoleCodes(Long userId) {
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        return roles == null || roles.isEmpty() ? List.of("user") : roles;
    }

    private List<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of("user");
        }
        List<String> validRoles = roles.stream()
                .map(String::trim)
                .filter(role -> role.equals("admin") || role.equals("user"))
                .distinct()
                .toList();
        if (validRoles.isEmpty()) {
            throw new ParamIllegalException("role must be admin or user");
        }
        return validRoles;
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setRoles(loadRoleCodes(user.getUserId()));
        return vo;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
