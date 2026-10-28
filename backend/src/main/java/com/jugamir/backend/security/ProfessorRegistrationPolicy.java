package com.jugamir.backend.security;

import org.springframework.stereotype.Component;
import com.jugamir.backend.exception.BusinessException;
import java.util.regex.Pattern;

@Component
public class ProfessorRegistrationPolicy {
    private static final Pattern INSTITUTIONAL_EMAIL = Pattern.compile("[^@\\s]+@ugr\\.es", Pattern.CASE_INSENSITIVE);

    public void requireInstitutionalEmail(String email) {
        if (email == null || !INSTITUTIONAL_EMAIL.matcher(email).matches())
            throw new BusinessException("Las cuentas de profesor deben usar un correo @ugr.es");
    }
}
