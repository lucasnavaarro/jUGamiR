package com.jugamir.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthRateLimiter {
    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public void check(String action, String identity, int maximum) {
        String key;
        try {
            key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                    (action + ":" + (identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT))).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        Integer hits = jdbc.queryForObject("""
            INSERT INTO security_rate_limits (bucket_key, hits, resets_at)
            VALUES (?, 1, CURRENT_TIMESTAMP + INTERVAL '10 minutes')
            ON CONFLICT (bucket_key) DO UPDATE SET
              hits = CASE WHEN security_rate_limits.resets_at <= CURRENT_TIMESTAMP THEN 1 ELSE security_rate_limits.hits + 1 END,
              resets_at = CASE WHEN security_rate_limits.resets_at <= CURRENT_TIMESTAMP THEN CURRENT_TIMESTAMP + INTERVAL '10 minutes' ELSE security_rate_limits.resets_at END
            RETURNING hits
            """, Integer.class, key);
        if (hits != null && hits > maximum)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espera diez minutos");
    }

    @Scheduled(fixedDelay = 600_000)
    public void purge() { jdbc.update("DELETE FROM security_rate_limits WHERE resets_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes'"); }
}
