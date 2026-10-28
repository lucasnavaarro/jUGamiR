package com.jugamir.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.jugamir.backend.exception.BusinessException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ProfessorRegistrationPolicy {
    private final Set<String> approvedEmails;

    public ProfessorRegistrationPolicy(@Value("${PROFESSOR_ALLOWED_EMAILS:}") String emails) {
        approvedEmails = Arrays.stream(emails.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public void requireApproved(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.endsWith("@ugr.es") || !approvedEmails.contains(normalized))
            throw new BusinessException("El registro de profesores requiere autorización previa del administrador");
    }
}
