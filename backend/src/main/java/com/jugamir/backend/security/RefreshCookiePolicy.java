package com.jugamir.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Component
public class RefreshCookiePolicy {
    private final boolean secure;
    public RefreshCookiePolicy(@Value("${app.cookie-secure:true}") boolean secure,
                               @Value("${app.url}") String appUrl, Environment environment) {
        URI uri = URI.create(appUrl);
        boolean loopback = Set.of("localhost", "127.0.0.1", "[::1]").contains(String.valueOf(uri.getHost()));
        if (!secure && (!loopback || !environment.acceptsProfiles(Profiles.of("local"))))
            throw new IllegalStateException("Las cookies sin Secure solo se permiten en el perfil local y localhost");
        if (!loopback && !"https".equals(uri.getScheme()))
            throw new IllegalStateException("APP_URL debe usar HTTPS fuera de localhost");
        this.secure = secure;
    }
    public ResponseCookie create(String value, Duration age) {
        return ResponseCookie.from("refreshToken", value).httpOnly(true).secure(secure)
                .path("/api/auth").sameSite("Strict").maxAge(age).build();
    }
}
