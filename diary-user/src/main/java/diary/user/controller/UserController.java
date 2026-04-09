package diary.user.controller;

import diary.config.result.ApiResponse;
import diary.dao.entity.user.dto.UserReqDTO;
import diary.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户控制器：注册、多种登录、RSA 公钥、手机验证码。
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.login(userDTO));
    }
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.register(userDTO));
    }
    @PostMapping("/verifycode")
    public ApiResponse<Map<String, Object>> verifyCode(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.verifyCode(userDTO));
    }

    @PostMapping("/resetPw")
    public ApiResponse<Map<String, Object>> resetPw(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.resetPw(userDTO));
    }
}
