package com.jugamir.backend.service;

import com.jugamir.backend.model.*;
import com.jugamir.backend.repository.*;
import com.jugamir.backend.security.AuthRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.security.authentication.BadCredentialsException;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginChallengeService {
    private final Codigo2faRepository codes;
    private final UsuarioRepository users;
    private final EmailService email;
    private final AuthRateLimiter limiter;

    @Transactional
    public String issue(Usuario user) {
        limiter.check("otp-send", user.getEmail(), 3);
        // Serialize logins for the same account and invalidate older challenges.
        users.findLockedById(user.getIdUsuario()).orElseThrow();
        codes.deleteByUsuario(user);
        String challenge = UUID.randomUUID().toString();
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        codes.saveAndFlush(Codigo2fa.builder().usuario(user).codigo(code).challengeId(challenge)
                .attempts(0).resends(0).lastSent(LocalDateTime.now())
                .expiraEn(LocalDateTime.now().plusMinutes(10)).usado(false).build());
        email.enviarCodigo(user.getEmail(), code);
        return challenge;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BadCredentialsException.class)
    public VerifiedIdentity consume(String challenge, String supplied) {
        Codigo2fa code = codes.findLockedByChallengeId(challenge).orElseThrow(this::invalid);
        check(code);
        code.setAttempts(code.getAttempts() + 1);
        if (!code.getCodigo().equals(supplied)) {
            if (code.getAttempts() >= 5) code.setUsado(true);
            codes.saveAndFlush(code);
            throw invalid();
        }
        code.setUsado(true);
        codes.saveAndFlush(code);
        return new VerifiedIdentity(code.getUsuario().getIdUsuario(), code.getUsuario().getTokenVersion());
    }

    @Transactional
    public void resend(String challenge) {
        Codigo2fa code = codes.findLockedByChallengeId(challenge).orElseThrow(this::invalid);
        check(code);
        if (code.getResends() >= 2 || code.getLastSent().isAfter(LocalDateTime.now().minusSeconds(60)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Espera antes de reenviar el código");
        limiter.check("otp-send", code.getUsuario().getEmail(), 3);
        code.setCodigo(String.format("%06d", new SecureRandom().nextInt(1_000_000)));
        code.setResends(code.getResends() + 1);
        code.setLastSent(LocalDateTime.now());
        // Do not reset attempts or extend the challenge's original expiry.
        codes.save(code);
        email.enviarCodigo(code.getUsuario().getEmail(), code.getCodigo());
    }

    public record VerifiedIdentity(Long userId, int tokenVersion) {}

    private void check(Codigo2fa code) {
        if (Boolean.TRUE.equals(code.getUsado()) || code.getAttempts() >= 5 || !code.getExpiraEn().isAfter(LocalDateTime.now())) throw invalid();
    }
    private BadCredentialsException invalid() { return new BadCredentialsException("Código incorrecto o desafío caducado. Inicia sesión de nuevo"); }
}
