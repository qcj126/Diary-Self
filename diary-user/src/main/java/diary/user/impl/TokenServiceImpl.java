package diary.user.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import diary.common.consts.RedisKeyConst;
import diary.common.entity.user.vo.TokenInfoVO;
import diary.common.entity.user.vo.TokenPairVO;
import diary.common.exception.CustomException;
import diary.user.service.TokenService;
import diary.utils.jwt.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public TokenPairVO issueTokenPair(String username, List<String> roles) {
        String accessToken = jwtUtil.generateAccessToken(username, roles);
        String refreshToken = jwtUtil.generateRefreshToken(username, roles);

        TokenInfoVO tokenInfo = TokenInfoVO.builder()
                .username(username)
                .roles(roles)
                .accessTokenId(jwtUtil.extractTokenId(accessToken))
                .refreshTokenId(jwtUtil.extractTokenId(refreshToken))
                .accessTokenExpireAt(toLocalDateTime(jwtUtil.extractExpiration(accessToken)))
                .refreshTokenExpireAt(toLocalDateTime(jwtUtil.extractExpiration(refreshToken)))
                .build();

        saveWhiteToken(tokenInfo.getAccessTokenId(), tokenInfo, jwtUtil.getAccessTokenExpirationMs());
        saveWhiteToken(tokenInfo.getRefreshTokenId(), tokenInfo, jwtUtil.getRefreshTokenExpirationMs());
        stringRedisTemplate.opsForSet().add(RedisKeyConst.USER_TOKEN_INDEX_PREFIX + username,
                tokenInfo.getAccessTokenId(), tokenInfo.getRefreshTokenId());
        stringRedisTemplate.expire(RedisKeyConst.USER_TOKEN_INDEX_PREFIX + username,
                jwtUtil.getRefreshTokenExpirationMs(), TimeUnit.MILLISECONDS);

        return TokenPairVO.builder()
                .tokenType("Bearer")
                .accessToken(accessToken)
                .accessTokenExpiresIn(jwtUtil.getAccessTokenExpirationMs() / 1000)
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn(jwtUtil.getRefreshTokenExpirationMs() / 1000)
                .build();
    }

    @Override
    public TokenPairVO refresh(String refreshToken) {
        try {
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new CustomException("refresh token type is invalid");
            }
            String tokenId = jwtUtil.extractTokenId(refreshToken);
            if (isBlack(tokenId) || !Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId))) {
                throw new CustomException("refresh token is invalid");
            }
            TokenInfoVO oldTokenInfo = readWhiteToken(tokenId);
            String username = jwtUtil.extractUsername(refreshToken);
            List<String> roles = jwtUtil.extractRoles(refreshToken);
            if (oldTokenInfo != null) {
                blackToken(oldTokenInfo.getAccessTokenId());
                stringRedisTemplate.delete(RedisKeyConst.TOKEN_WHITE_PREFIX + oldTokenInfo.getAccessTokenId());
            }
            blackToken(tokenId);
            stringRedisTemplate.delete(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId);
            return issueTokenPair(username, roles);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("refresh token check failed");
        }
    }

    @Override
    public boolean validateAccessToken(String accessToken) {
        try {
            String tokenId = jwtUtil.extractTokenId(accessToken);
            return jwtUtil.isAccessToken(accessToken)
                    && !isBlack(tokenId)
                    && Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<TokenInfoVO> queryTokenInfo(String username) {
        Set<String> tokenIds = stringRedisTemplate.opsForSet().members(RedisKeyConst.USER_TOKEN_INDEX_PREFIX + username);
        if (tokenIds == null || tokenIds.isEmpty()) {
            return List.of();
        }
        List<TokenInfoVO> result = new ArrayList<>();
        for (String tokenId : tokenIds) {
            String json = stringRedisTemplate.opsForValue().get(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId);
            if (json == null) {
                continue;
            }
            try {
                TokenInfoVO tokenInfo = objectMapper.readValue(json, TokenInfoVO.class);
                if (result.stream().noneMatch(item -> Objects.equals(item.getAccessTokenId(), tokenInfo.getAccessTokenId()))) {
                    result.add(tokenInfo);
                }
            } catch (Exception ignored) {
                // Skip one broken Redis value without hiding other active tokens.
            }
        }
        return result;
    }

    @Override
    public void kickOut(String username) {
        Set<String> tokenIds = stringRedisTemplate.opsForSet().members(RedisKeyConst.USER_TOKEN_INDEX_PREFIX + username);
        if (tokenIds == null || tokenIds.isEmpty()) {
            return;
        }
        for (String tokenId : tokenIds) {
            blackToken(tokenId);
            stringRedisTemplate.delete(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId);
        }
        stringRedisTemplate.delete(RedisKeyConst.USER_TOKEN_INDEX_PREFIX + username);
    }

    private void saveWhiteToken(String tokenId, TokenInfoVO tokenInfo, long ttlMs) {
        try {
            stringRedisTemplate.opsForValue().set(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId,
                    objectMapper.writeValueAsString(tokenInfo), ttlMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new CustomException("write token to redis failed");
        }
    }

    private boolean isBlack(String tokenId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConst.TOKEN_BLACK_PREFIX + tokenId));
    }

    private TokenInfoVO readWhiteToken(String tokenId) {
        String json = stringRedisTemplate.opsForValue().get(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TokenInfoVO.class);
        } catch (Exception e) {
            return null;
        }
    }

    private void blackToken(String tokenId) {
        if (tokenId == null) {
            return;
        }
        Long ttl = stringRedisTemplate.getExpire(RedisKeyConst.TOKEN_WHITE_PREFIX + tokenId, TimeUnit.MILLISECONDS);
        long blackTtl = ttl == null || ttl <= 0 ? jwtUtil.getRefreshTokenExpirationMs() : ttl;
        stringRedisTemplate.opsForValue().set(RedisKeyConst.TOKEN_BLACK_PREFIX + tokenId, "1", blackTtl, TimeUnit.MILLISECONDS);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }
}
