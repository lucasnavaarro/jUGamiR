package com.jugamir.backend.repository;

import com.jugamir.backend.model.PasswordResetToken;
import com.jugamir.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsadoFalse(String token);

    @Modifying
    @Query("update PasswordResetToken p set p.usado = true where p.token = :token and p.usado = false and p.expiraEn > :now")
    int consume(@Param("token") String token, @Param("now") java.time.LocalDateTime now);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PasswordResetToken p WHERE p.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") Usuario usuario);
}
