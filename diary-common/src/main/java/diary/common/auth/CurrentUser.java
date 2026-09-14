package diary.common.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provides the authenticated user identity propagated by the gateway.
 */
@Component
public class CurrentUser {
    private static final String USER_ID_HEADER = "X-Auth-User-Id";
    private final HttpServletRequest request;

    public CurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns the current authenticated user's ID.
     */
    public Long getUser() {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw unauthorized("Missing authenticated user identity");
        }

        try {
            long userId = Long.parseLong(userIdHeader);
            if (userId <= 0) {
                throw unauthorized("Invalid authenticated user identity");
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw unauthorized("Invalid authenticated user identity");
        }
    }

    private ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }
}
