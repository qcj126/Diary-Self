package diary.utils.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${jwt.secret:your256bitsecretkeymustbeatleast32characterslong}")
    private String secret;

    @Value("${jwt.access-token-expiration:600000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration:21600000}")
    private long refreshTokenExpirationMs;

    private static final String CLAIM_ROLES = "roles";

    private static final String CLAIM_TOKEN_TYPE = "token_type";

    public String generateAccessToken(String username, List<String> roles) {
        return generateToken(username, roles, "access", accessTokenExpirationMs);
    }

    public String generateRefreshToken(String username, List<String> roles) {
        return generateToken(username, roles, "refresh", refreshTokenExpirationMs);
    }

    /**
     * Keep the old method compatible with existing callers.
     */
    public String generateToken(String username) {
        return generateAccessToken(username, List.of("user"));
    }

    private String generateToken(String username, List<String> roles, String tokenType, long expirationMs) {
        return Jwts.builder()
                .setSubject(username)
                .setId(UUID.randomUUID().toString())
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractTokenId(String token) {
        return extractClaims(token).getId();
    }

    public String extractTokenType(String token) {
        return extractClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = extractClaims(token).get(CLAIM_ROLES);
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    public boolean isAccessToken(String token) {
        return "access".equals(extractTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractTokenType(token));
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
