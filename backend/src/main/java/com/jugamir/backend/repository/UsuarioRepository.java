package com.jugamir.backend.repository;

import com.jugamir.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where lower(trim(u.email)) = lower(trim(:email))")
    Optional<Usuario> findByEmail(@org.springframework.data.repository.query.Param("email") String email);

    @org.springframework.data.jpa.repository.Query("select (count(u) > 0) from Usuario u where lower(trim(u.email)) = lower(trim(:email))")
    boolean existsByEmail(@org.springframework.data.repository.query.Param("email") String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.idUsuario = :id")
    Optional<Usuario> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);

}
