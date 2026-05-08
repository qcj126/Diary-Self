package diary.user.service.impl;

import diary.common.entity.user.dto.UserReqDTO;
import diary.common.entity.user.po.User;
import diary.common.exception.CustomException;
import diary.common.exception.ParamIllegalException;
import diary.common.exception.SameDataException;
import diary.dao.mapper.user.UserMapper;
import diary.dao.redis.ManageVerifyCode;
import diary.user.service.UserService;
import diary.utils.commonutil.MyUtils;
import diary.utils.jwt.JwtUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static diary.utils.crypto.BCryptUtilNoSpring.encode;
import static diary.utils.crypto.BCryptUtilNoSpring.matches;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private ManageVerifyCode manageVerifyCode;

    @Override
    public Map<String, Object> login(UserReqDTO userDTO) {
        // 校验登录类型
        if (userDTO.getType() == null) {
            throw new ParamIllegalException("登录类型不能为空");
        }
        User user = null;
        // 根据登录类型处理不同逻辑
        if (userDTO.getType() != 1 && userDTO.getType() != 2) {
            throw new ParamIllegalException("不支持的登录类型");
        }
        // 账号密码登录
        user = loginByPassword(userDTO);
        // 生成 Token
        String token = jwtUtil.generateToken(user.getUsername());

        return Map.of("token", token);
    }

    /**
     * 账号密码登录
     */
    private User loginByPassword(UserReqDTO userDTO) {
        // 参数校验
        if (userDTO.getUsername() == null && userDTO.getEmail() == null && userDTO.getPhone() == null) {
            throw new ParamIllegalException("用户名、邮箱或手机号至少提供一个");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new ParamIllegalException("密码不能为空");
        }

        // 查询用户
        User user = loadUserByIdentify(userDTO);
        
        // 验证密码
        if (!matches(userDTO.getPassword(), user.getPassword())) {
            throw new CustomException("密码错误");
        }
        
        return user;
    }

    /**
     * 验证码登录
     */
    private User loginByCode(UserReqDTO userDTO) {
        // 参数校验
        if (userDTO.getPhone() == null || userDTO.getPhone().trim().isEmpty()) {
            throw new RuntimeException("手机号不能为空");
        }
        if (userDTO.getCode() == null || userDTO.getCode().trim().isEmpty()) {
            throw new RuntimeException("验证码不能为空");
        }
        // 验证验证码
        if (!manageVerifyCode.checkVerifyCode(userDTO.getCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 查询用户
        User user = loadUserByUsername(userDTO.getPhone());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        return user;
    }

    /**
     * 根据标识符（用户名/邮箱/手机号）加载用户
     */
    private User loadUserByIdentify(UserReqDTO userDTO) {
        User user = null;
        
        if (userDTO.getEmail() != null && !userDTO.getEmail().trim().isEmpty()) {
            user =  loadUserByUsername(userDTO.getEmail());
        } else if (userDTO.getUsername() != null && !userDTO.getUsername().trim().isEmpty()) {
            user = loadUserByUsername(userDTO.getUsername());
        } else if (userDTO.getPhone() != null && !userDTO.getPhone().trim().isEmpty()) {
            user = loadUserByUsername(userDTO.getPhone());
        }
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        return user;
    }

    @Override
    public String register(UserReqDTO userDTO) {
        // 参数校验：username、email、phone、password 一个都不能为空
        if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
            throw new ParamIllegalException("用户名不能为空");
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            throw new ParamIllegalException("邮箱不能为空");
        }
        if (userDTO.getPhone() == null || userDTO.getPhone().trim().isEmpty()) {
            throw new ParamIllegalException("手机号不能为空");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new ParamIllegalException("密码不能为空");
        }
    
        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(userDTO.getUsername());
        if (existUser != null) {
            throw new SameDataException("用户名已被注册");
        }
    
        // 检查邮箱是否已存在
        existUser = userMapper.selectByEmail(userDTO.getEmail());
        if (existUser != null) {
            throw new SameDataException("邮箱已被注册");
        }
    
        // 检查手机号是否已存在
        existUser = userMapper.selectByPhone(userDTO.getPhone());
        if (existUser != null) {
            throw new SameDataException("手机号已被注册");
        }
    
        // 创建用户对象
        User newUser = new User();
        newUser.setUserId(MyUtils.getPrimaryKey());
        newUser.setUsername(userDTO.getUsername());
        newUser.setEmail(userDTO.getEmail());
        newUser.setPhone(userDTO.getPhone());
        // 使用 BCrypt 加密密码
        newUser.setPassword(encode(userDTO.getPassword()));
    
        // 插入数据库
        int result = userMapper.userRegister(newUser);
        if (result <= 0) {
            throw new CustomException("注册失败");
        }
        log.info("用户注册成功: {}", userDTO.getUsername());
        return "注册成功";
    }

    @Override
    public Map<String, Object> resetPw(UserReqDTO userDTO) {
        User user = null;
        if (userDTO.getUsername() == null && userDTO.getEmail() == null && userDTO.getPhone() == null && userDTO.getPassword() == null)
            throw new RuntimeException("用户名或邮箱或手机号或密码不能为空");
        if (userDTO.getEmail() != null)
            user = loadUserByUsername(userDTO.getEmail());
        if (userDTO.getUsername() != null)
            user = loadUserByUsername(userDTO.getUsername());
        if (userDTO.getPhone() != null)
            user = loadUserByUsername(userDTO.getPhone());
        if (user == null) throw new RuntimeException("用户不存在");
        userMapper.updatePassword(user.getUsername(), encode(userDTO.getPassword()));
        return Map.of(
                "message", "密码重置成功"
        );
    }

    @Override
    public Map<String, Object> verifyCode(UserReqDTO userDTO) {
        User user = null;
        if (userDTO.getPhone() != null)
            user = loadUserByUsername(userDTO.getPhone());
        if (user == null) throw new RuntimeException("用户不存在");
        // 生成验证码
        String code = String.valueOf(Math.random()).substring(2, 8);
        manageVerifyCode.setVerifyCode(code);
        return Map.of("message", "验证码已发送");
    }

    public User loadUserByUsername(String identify) {
        User user = null;
        if (identify.contains("@")) {
            user = userMapper.selectByEmail(identify);
        } else if (identify.matches("^1[3-9]\\d{9}$")) {
            user = userMapper.selectByPhone(identify);
        } else {
            user = userMapper.selectByUsername(identify);
        }
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setRoles(List.of("ROLE_USER"));
        return user;
    }
}

