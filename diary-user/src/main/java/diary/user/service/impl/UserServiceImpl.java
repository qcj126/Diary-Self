package diary.user.service.impl;

import diary.config.redis.config.verifycode.ManageVerifyCode;
import diary.config.result.RegisterException;
import diary.config.security.detail.SecurityUserDetails;
import diary.config.security.detailservice.MyUserDetailsService;
import diary.dao.entity.user.dto.UserReqDTO;
import diary.dao.entity.user.po.User;
import diary.dao.mapper.user.UserMapper;
import diary.user.service.UserService;
import diary.utils.jwt.JwtUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private MyUserDetailsService userDetailsService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private UserMapper userMapper;

    @Resource
    private ManageVerifyCode manageVerifyCode;

    @Override
    public Map<String, Object> login(UserReqDTO userDTO) {
        // 校验登录类型
        if (userDTO.getType() == null) {
            throw new RuntimeException("登录类型不能为空");
        }
        
        SecurityUserDetails user = null;
        
        // 根据登录类型处理不同逻辑
        if (userDTO.getType() == 1) {
            // 账号密码登录
            user = loginByPassword(userDTO);
        } else if (userDTO.getType() == 2) {
            // 验证码登录
            user = loginByCode(userDTO);
        } else {
            throw new RuntimeException("不支持的登录类型");
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getUsername());

        return Map.of(
                "token", token
        );
    }

    /**
     * 账号密码登录
     */
    private SecurityUserDetails loginByPassword(UserReqDTO userDTO) {
        // 参数校验
        if (userDTO.getUsername() == null && userDTO.getEmail() == null && userDTO.getPhone() == null) {
            throw new RuntimeException("用户名、邮箱或手机号至少提供一个");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 查询用户
        SecurityUserDetails user = loadUserByIdentify(userDTO);
        
        // 验证密码
        if (!passwordEncoder.matches(userDTO.getPassword(), user.getUser().getPassword())) {
            throw new RuntimeException("密码错误");
        }
        
        return user;
    }

    /**
     * 验证码登录
     */
    private SecurityUserDetails loginByCode(UserReqDTO userDTO) {
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
        SecurityUserDetails user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getPhone());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        return user;
    }

    /**
     * 根据标识符（用户名/邮箱/手机号）加载用户
     */
    private SecurityUserDetails loadUserByIdentify(UserReqDTO userDTO) {
        SecurityUserDetails user = null;
        
        if (userDTO.getEmail() != null && !userDTO.getEmail().trim().isEmpty()) {
            user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getEmail());
        } else if (userDTO.getUsername() != null && !userDTO.getUsername().trim().isEmpty()) {
            user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getUsername());
        } else if (userDTO.getPhone() != null && !userDTO.getPhone().trim().isEmpty()) {
            user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getPhone());
        }
        
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        return user;
    }

    @Override
    public Map<String, Object> register(UserReqDTO userDTO) {
        // 参数校验：username、email、phone、password 一个都不能为空
        if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
            throw new RegisterException("用户名不能为空");
        }
        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            throw new RegisterException("邮箱不能为空");
        }
        if (userDTO.getPhone() == null || userDTO.getPhone().trim().isEmpty()) {
            throw new RegisterException("手机号不能为空");
        }
        if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
            throw new RegisterException("密码不能为空");
        }
    
        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(userDTO.getUsername());
        if (existUser != null) {
            throw new RegisterException("用户名已存在");
        }
    
        // 检查邮箱是否已存在
        existUser = userMapper.selectByEmail(userDTO.getEmail());
        if (existUser != null) {
            throw new RegisterException("邮箱已被注册");
        }
    
        // 检查手机号是否已存在
        existUser = userMapper.selectByPhone(userDTO.getPhone());
        if (existUser != null) {
            throw new RegisterException("手机号已被注册");
        }
    
        // 创建用户对象
        User newUser = new User();
        newUser.setUserId(Long.parseLong(String.valueOf(System.nanoTime()).substring(0, 15)));
        newUser.setUsername(userDTO.getUsername());
        newUser.setEmail(userDTO.getEmail());
        newUser.setPhone(userDTO.getPhone());
        // 使用 PasswordEncoder 加密密码
        newUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
    
        // 插入数据库
        int result = userMapper.userRegister(newUser);
        if (result <= 0) {
            throw new RegisterException("注册失败");
        }
    
        log.info("用户注册成功: {}", userDTO.getUsername());
    
        return Map.of(
                "message", "注册成功"
        );
    }

    @Override
    public Map<String, Object> resetPw(UserReqDTO userDTO) {
        SecurityUserDetails user = null;
        if (userDTO.getUsername() == null && userDTO.getEmail() == null && userDTO.getPhone() == null && userDTO.getPassword() == null)
            throw new RuntimeException("用户名或邮箱或手机号或密码不能为空");
        if (userDTO.getEmail() != null)
            user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getEmail());
        if (userDTO.getUsername() != null)
            user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getUsername());
        if (userDTO.getPhone() != null)
            user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getPhone());
        if (user == null) throw new RuntimeException("用户不存在");
        userMapper.updatePassword(user.getUsername(), passwordEncoder.encode(userDTO.getPassword()));
        return Map.of(
                "message", "密码重置成功"
        );
    }

    @Override
    public Map<String, Object> verifyCode(UserReqDTO userDTO) {
        SecurityUserDetails user = null;
        if (userDTO.getPhone() != null)
            user = (SecurityUserDetails) userDetailsService.loadUserByUsername(userDTO.getPhone());
        if (user == null) throw new RuntimeException("用户不存在");
        // 生成验证码
        String code = String.valueOf(Math.random()).substring(2, 8);
        manageVerifyCode.setVerifyCode(code);
        return Map.of("message", "验证码已发送");
    }
}

