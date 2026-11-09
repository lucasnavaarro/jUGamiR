package com.jugamir.backend.security;

import com.jugamir.backend.dto.RegisterJugadorRequest;
import com.jugamir.backend.dto.RegisterProfesorRequest;
import com.jugamir.backend.dto.ResetPasswordRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordPolicyTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll static void closeValidatorFactory() { FACTORY.close(); }

    private void assertAcceptedByAllFlows(String password, boolean accepted) {
        assertEquals(accepted, VALIDATOR.validateValue(RegisterJugadorRequest.class, "password", password).isEmpty(), "player registration");
        assertEquals(accepted, VALIDATOR.validateValue(RegisterProfesorRequest.class, "password", password).isEmpty(), "professor registration");
        assertEquals(accepted, VALIDATOR.validateValue(ResetPasswordRequest.class, "nuevaPassword", password).isEmpty(), "password reset");
    }

    @Test void acceptsDotAndEveryOtherPrintableAsciiSymbolInAllFlows() {
        for (char symbol = 33; symbol <= 126; symbol++) {
            if (!Character.isLetterOrDigit(symbol)) assertAcceptedByAllFlows("Aa12345" + symbol, true);
        }
    }

    @Test void rejectsMissingRequirementsWhitespaceAndNonAsciiCharactersInAllFlows() {
        for (String password : new String[]{null, "", "Aa1234.", "aa12345.", "AA12345.", "Abcdefg.", "Aa123456", "Aa12345 ", "Aa12345. ", "Aa12345.\n", "Aa12345.\r", "Aa12345.\t", "Aa12345.\u00a0", "Aa12345.ñ"}) {
            assertAcceptedByAllFlows(password, false);
        }
    }
}
