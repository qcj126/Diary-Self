package diary.config.filter;

import diary.config.security.detail.SecurityUserDetails;
import diary.dao.entity.user.po.User;
import diary.dao.mapper.user.UserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    @Resource
    private UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // 去掉 "Bearer "

            try {
                // 解析 token: 格式为 "username_timestamp"
                int lastUnderscore = token.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String username = token.substring(0, lastUnderscore);

                    // 重新加载用户（或从缓存获取）
                    User user = userMapper.selectByUsername(username); // 通过 username 查询用户
                    SecurityUserDetails userDetails = new SecurityUserDetails(user);
                    if (user != null) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception e) {
                // token 无效，忽略
            }
        }

        filterChain.doFilter(request, response);
    }
}