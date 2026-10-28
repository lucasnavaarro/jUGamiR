package com.jugamir.backend.security;
import org.junit.jupiter.api.Test;
import com.jugamir.backend.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
class ProfessorRegistrationPolicyTest {
    @Test void deniesUnapprovedAndNonInstitutionalAccounts() {
        var policy = new ProfessorRegistrationPolicy("teacher@ugr.es, external@example.test");
        assertDoesNotThrow(() -> policy.requireApproved("TEACHER@ugr.es"));
        assertThrows(BusinessException.class, () -> policy.requireApproved("student@ugr.es"));
        assertThrows(BusinessException.class, () -> policy.requireApproved("external@example.test"));
        assertThrows(BusinessException.class, () -> new ProfessorRegistrationPolicy("").requireApproved("teacher@ugr.es"));
    }
}
