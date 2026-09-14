package diary.common.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserTest {

    @Test
    void returnsCurrentUserIdFromTrustedHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-User-Id", "10001");

        assertEquals(10001L, new CurrentUser(request).getUser());
    }

    @Test
    void rejectsMissingUserIdentity() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new CurrentUser(new MockHttpServletRequest()).getUser());

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void rejectsInvalidUserIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-User-Id", "not-a-number");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new CurrentUser(request).getUser());

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
