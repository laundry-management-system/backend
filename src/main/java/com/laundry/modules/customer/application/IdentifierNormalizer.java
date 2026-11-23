package com.laundry.modules.customer.application;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class IdentifierNormalizer {

    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-]");

    private IdentifierNormalizer() {
    }

    public static Optional<String> normalizePhone(String input) {
        if (input == null) return Optional.empty();
        String digits = SEPARATORS.matcher(input).replaceAll("");
        if (digits.startsWith("+84")) digits = "0" + digits.substring(3);
        else if (digits.startsWith("84") && digits.length() == 11) digits = "0" + digits.substring(2);
        return PHONE.matcher(digits).matches() ? Optional.of(digits) : Optional.empty();
    }

    public static String normalizeEmail(String input) {
        if (input == null || input.isBlank()) return null;
        return input.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isEmail(String input) {
        return input != null && input.contains("@");
    }

    public static boolean isValidEmail(String normalized) {
        return normalized != null && normalized.length() <= 255 && EMAIL.matcher(normalized).matches();
    }

    /** Ít nhất 8 ký tự, có chữ và số; áp cho đăng ký, đổi và đặt lại mật khẩu. */
    public static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 8
                && !password.isBlank()
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
    }
}
