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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

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
        String tokenId;
        List<String> roles;
        try {
            if (!jwtUtil.isAccessToken(accessToken)) {
                return unauthorized(exchange);
            }
            username = jwtUtil.extractUsername(accessToken);
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

                    // Downstream services can read these trusted headers directly.
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-Auth-Username", username)
                            .header("X-Auth-Roles", String.join(",", roles))
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean shouldSkipAuth(String path) {
        return authProperties.getExcludePaths().stream().anyMatch(excludePath ->
                path.equals(excludePath) || path.startsWith(excludePath + "/"));
    }

    private boolean requiresAdmin(String path) {
        return path.contains("/add") || path.contains("/delete");
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
