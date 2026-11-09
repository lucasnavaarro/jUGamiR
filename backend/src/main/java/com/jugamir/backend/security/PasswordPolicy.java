package com.jugamir.backend.security;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    // Printable ASCII includes every punctuation symbol, not just a short allowlist.
    // The final lookahead requires the absolute end, including in JavaScript.
    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9])[\\x21-\\x7E]{8,}(?![\\s\\S])";
    public static final String MESSAGE = "Usa al menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo (por ejemplo, . ! @ #). Usa letras de la A a la Z, sin espacios.";
}
