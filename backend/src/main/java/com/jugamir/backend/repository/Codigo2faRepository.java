package com.jugamir.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.jugamir.backend.model.Codigo2fa;
import java.util.Optional;
import com.jugamir.backend.model.Usuario;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.repository.query.Param;

public interface Codigo2faRepository extends JpaRepository<Codigo2fa, Long> {

    Optional<Codigo2fa> findByUsuarioAndCodigoAndUsadoFalse(Usuario usuario, String codigo);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Codigo2fa c WHERE c.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") Usuario usuario);
}
