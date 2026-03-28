package com.jugamir.backend.repository;

import com.jugamir.backend.model.RefreshToken;
import com.jugamir.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token); // Busca el refresh token por token

    Optional<RefreshToken> findByUsuario(Usuario usuario); // Busca el refresh token de un usuario

    void deleteByUsuario(Usuario usuario); // Borra todos los refresh tokens de un usuario, antes de generar nuevos

    void deleteByToken(String token); // Borra un refresh token específico. LogOut
}
