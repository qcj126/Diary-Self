package diary.gateway.filter;

import diary.common.consts.RedisKeyConst;
import diary.gateway.config.AuthProperties;
import diary.utils.jwt.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final List<String> ADMIN_PATHS = List.of(
            "/user/add",
            "/user/delete",
            "/user/token/**"
    );

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Resource
    private AuthProperties authProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        String path = request.getURI().getPath();
        if (shouldSkipAuth(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String accessToken = authHeader.substring(7);
        String username;
        Long userId;
        String tokenId;
        List<String> roles;
        try {
            if (!jwtUtil.isAccessToken(accessToken)) {
                return unauthorized(exchange);
            }
            username = jwtUtil.extractUsername(accessToken);
            userId = jwtUtil.extractUserId(accessToken);
            if (userId == null) {
                return unauthorized(exchange);
            }
            tokenId = jwtUtil.extractTokenId(accessToken);
            roles = jwtUtil.extractRoles(accessToken);
        } catch (Exception e) {
            return unauthorized(exchange);
        }

        Mono<Boolean> inWhite = reactiveStringRedisTemplate.hasKey(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId);
        Mono<Boolean> inBlack = reactiveStringRedisTemplate.hasKey(RedisKeyConst.TOKEN_BLACK_PREFIX + tokenId);

        return Mono.zip(inWhite, inBlack)
                .flatMap(tuple -> {
                    boolean validToken = Boolean.TRUE.equals(tuple.getT1()) && !Boolean.TRUE.equals(tuple.getT2());
                    if (!validToken) {
                        return unauthorized(exchange);
                    }
                    if (requiresAdmin(path) && !roles.contains("admin")) {
                        return forbidden(exchange);
                    }

                    /*
                     * 改前：AI 服务把 userId 固定为 10000，查询又只按 taskId，无法做真正的数据归属校验。
                     * 改后：JWT 携带 user_id，网关先删除客户端可能伪造的身份头，再写入可信身份头。
                     * 效果：下游可使用 taskId + userId 查询，避免横向越权。
                     */
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .headers(headers -> {
                                headers.remove("X-Auth-User-Id");
                                headers.remove("X-Auth-Username");
                                headers.remove("X-Auth-Roles");
                                headers.set("X-Auth-User-Id", userId.toString());
                                headers.set("X-Auth-Username", username);
                                headers.set("X-Auth-Roles", String.join(",", roles));
                            })
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean shouldSkipAuth(String path) {
        return authProperties.getExcludePaths().stream().anyMatch(excludePath ->
                PATH_MATCHER.match(excludePath, path) || path.equals(excludePath) || path.startsWith(excludePath + "/"));
    }

    private boolean requiresAdmin(String path) {
        return ADMIN_PATHS.stream().anyMatch(adminPath -> PATH_MATCHER.match(adminPath, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
