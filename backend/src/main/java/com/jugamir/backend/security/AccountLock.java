package com.jugamir.backend.security;

import com.jugamir.backend.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountLock {
    private final EntityManager entities;
    // Refresh as well as lock: authentication may have loaded a stale entity earlier.
    public Usuario current(Long id) {
        Usuario user = entities.find(Usuario.class, id);
        if (user == null) throw new org.springframework.security.authentication.BadCredentialsException("Cuenta no disponible");
        entities.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }
}
