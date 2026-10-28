package com.jugamir.backend.service;

import com.jugamir.backend.model.Usuario;
import com.jugamir.backend.repository.*;
import com.jugamir.backend.security.*;
import com.jugamir.backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class EmailChangeService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final AuthRateLimiter limiter;
    private final UsuarioRepository users;
    private final com.jugamir.backend.security.AccountLock accounts;
    private final ProfesorRepository professors;
    private final ProfessorRegistrationPolicy professorPolicy;
    private final EmailService mail;

    private void reauthenticate(Usuario user, String password) {
        limiter.check("email-password", user.getIdUsuario().toString(), 5);
        if (password == null || !passwords.matches(password, user.getContrasenaHash()))
            throw new BusinessException("La contraseña actual no es correcta");
    }

    @Transactional
    public void issue(Long userId, String newEmail, String password) {
        Usuario user = accounts.current(userId);
        reauthenticate(user, password);
        limiter.check("email-send", userId.toString(), 3);
        if (users.existsByEmail(newEmail)) throw new BusinessException("El email ya está registrado");
        if (professors.existsById(userId)) professorPolicy.requireApproved(newEmail);
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        jdbc.update("""
            INSERT INTO email_change_challenges (usuario_id, new_email, code_hash, token_version, expires_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '10 minutes')
            ON CONFLICT (usuario_id) DO UPDATE SET new_email = EXCLUDED.new_email, code_hash = EXCLUDED.code_hash,
              token_version = EXCLUDED.token_version, expires_at = EXCLUDED.expires_at
            """, userId, newEmail, hash(code), user.getTokenVersion());
        mail.enviarCodigoCambioEmail(newEmail, code);
    }

    @Transactional
    public void consume(Usuario user, String newEmail, String password, String code) {
        reauthenticate(user, password);
        limiter.check("email-verify", user.getIdUsuario().toString(), 5);
        int deleted = jdbc.update("""
            DELETE FROM email_change_challenges WHERE usuario_id = ? AND new_email = ? AND code_hash = ?
              AND token_version = ? AND expires_at > CURRENT_TIMESTAMP
            """, user.getIdUsuario(), newEmail, hash(code == null ? "" : code), user.getTokenVersion());
        if (deleted != 1) throw new BusinessException("Verifica el nuevo correo con un código vigente");
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
