package diary.user.controller;

import diary.common.entity.user.dto.KickOutDTO;
import diary.common.entity.user.dto.RefreshTokenDTO;
import diary.common.entity.user.dto.UserReqDTO;
import diary.common.entity.user.vo.TokenInfoVO;
import diary.common.entity.user.vo.TokenPairVO;
import diary.common.entity.user.vo.UserVO;
import diary.common.result.ApiResponse;
import diary.config.logconfig.OperLog;
import diary.user.service.TokenService;
import diary.user.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private TokenService tokenService;

    @OperLog(module = "用户管理", description = "用户登录", operationType = "LOGIN", saveParams = false, saveResult = false)
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.login(userDTO));
    }

    @OperLog(module = "用户管理", description = "刷新访问令牌", operationType = "REFRESH", saveParams = false, saveResult = false)
    @PostMapping("/refresh")
    public ApiResponse<TokenPairVO> refresh(@RequestBody RefreshTokenDTO refreshTokenDTO) {
        return ApiResponse.success(userService.refresh(refreshTokenDTO.getRefreshToken()));
    }

    @OperLog(module = "用户管理", description = "用户注册", operationType = "INSERT", saveParams = false, saveResult = false)
    @PostMapping("/register")
    public ApiResponse<String> register(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.register(userDTO));
    }

    @PreAuthorize("hasRole('admin')")
    @OperLog(module = "用户管理", description = "管理员新增用户", operationType = "INSERT", saveParams = false, saveResult = true)
    @PostMapping("/add")
    public ApiResponse<String> addUser(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.addUser(userDTO));
    }

    @PreAuthorize("hasRole('admin')")
    @OperLog(module = "用户管理", description = "管理员删除用户", operationType = "DELETE", saveResult = true)
    @PostMapping("/delete")
    public ApiResponse<String> deleteUser(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.deleteUser(userDTO.getUsername()));
    }

    @PreAuthorize("hasAnyRole('admin','user')")
    @OperLog(module = "用户管理", description = "查询用户列表", operationType = "SELECT", saveResult = true)
    @GetMapping("/query")
    public ApiResponse<List<UserVO>> queryUsers() {
        return ApiResponse.success(userService.queryUsers());
    }

    @OperLog(module = "用户管理", description = "发送验证码", operationType = "SEND", saveParams = false, saveResult = false)
    @PostMapping("/verifycode")
    public ApiResponse<Map<String, Object>> verifyCode(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.verifyCode(userDTO));
    }

    @OperLog(module = "用户管理", description = "重置用户密码", operationType = "UPDATE", saveParams = false, saveResult = false)
    @PostMapping("/resetPw")
    public ApiResponse<Map<String, Object>> resetPw(@RequestBody UserReqDTO userDTO) {
        return ApiResponse.success(userService.resetPw(userDTO));
    }

    @PreAuthorize("hasRole('admin')")
    @OperLog(module = "用户管理", description = "查询用户令牌信息", operationType = "SELECT", saveResult = false)
    @GetMapping("/token/query")
    public ApiResponse<List<TokenInfoVO>> queryTokenInfo(@RequestParam String username) {
        return ApiResponse.success(tokenService.queryTokenInfo(username));
    }

    @PreAuthorize("hasRole('admin')")
    @OperLog(module = "用户管理", description = "强制用户下线", operationType = "KICK_OUT", saveResult = true)
    @PostMapping("/token/kickout")
    public ApiResponse<String> kickOut(@RequestBody KickOutDTO kickOutDTO) {
        tokenService.kickOut(kickOutDTO.getUsername());
        return ApiResponse.success("kick out success");
    }
}
