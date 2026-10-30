// Keep this rule and message aligned with backend/security/PasswordPolicy.java.
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9])[\x21-\x7E]{8,}(?![\s\S])/;

export const PASSWORD_REQUIREMENTS = 'Usa al menos 8 caracteres, una mayúscula, una minúscula, un número y un símbolo (por ejemplo, . ! @ #). Usa letras de la A a la Z, sin espacios.';

export function isValidPassword(password) {
    return typeof password === 'string' && PASSWORD_PATTERN.test(password);
}
