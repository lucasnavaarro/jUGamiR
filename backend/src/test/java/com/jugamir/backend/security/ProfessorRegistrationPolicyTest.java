package com.jugamir.backend.security;
import org.junit.jupiter.api.Test;
import com.jugamir.backend.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
class ProfessorRegistrationPolicyTest {
    private final ProfessorRegistrationPolicy policy = new ProfessorRegistrationPolicy();

    @Test void acceptsAnyAddressAtTheExactInstitutionalDomain() {
        for (String email : new String[]{"teacher@ugr.es", "another.person@ugr.es", "teacher+alias@ugr.es", "TEACHER@UGR.ES"})
            assertDoesNotThrow(() -> policy.requireInstitutionalEmail(email), email);
    }

    @Test void rejectsOtherDomainsAndLookalikes() {
        for (String email : new String[]{"teacher@gmail.com", "teacher@mail.ugr.es", "teacher@notugr.es", "teacher@ugr.es.example.com"})
            assertThrows(BusinessException.class, () -> policy.requireInstitutionalEmail(email), email);
    }

    @Test void rejectsEmptyOrMalformedAddresses() {
        for (String email : new String[]{null, "", "@ugr.es", "teacher@@ugr.es", "teacher name@ugr.es", "teacher@ugr.es\n"})
            assertThrows(BusinessException.class, () -> policy.requireInstitutionalEmail(email));
    }
}
