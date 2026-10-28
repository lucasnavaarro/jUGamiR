package com.jugamir.backend.security;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
class RefreshCookiePolicyTest {
    @Test void secureCookiesAndDeletionUseSameFlags() {
        var policy = new RefreshCookiePolicy(true, "https://game.example", new MockEnvironment());
        for (var duration : new Duration[]{Duration.ofDays(30), Duration.ZERO}) {
            var cookie = policy.create("test", duration);
            assertTrue(cookie.isSecure()); assertTrue(cookie.isHttpOnly());
            assertEquals("Strict", cookie.getSameSite()); assertEquals("/api/auth", cookie.getPath());
        }
    }
    @Test void insecureCookieRequiresLocalProfileAndLoopback() {
        var local = new MockEnvironment().withProperty("spring.profiles.active", "local");
        assertFalse(new RefreshCookiePolicy(false, "http://localhost:3000", local).create("", Duration.ZERO).isSecure());
        assertThrows(IllegalStateException.class, () -> new RefreshCookiePolicy(false, "https://game.example", local));
        assertThrows(IllegalStateException.class, () -> new RefreshCookiePolicy(false, "http://localhost:3000", new MockEnvironment()));
        assertThrows(IllegalStateException.class, () -> new RefreshCookiePolicy(true, "http://game.example", new MockEnvironment()));
    }
}
