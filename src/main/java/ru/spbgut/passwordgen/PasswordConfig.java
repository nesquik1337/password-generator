package ru.spbgut.passwordgen;

import java.util.EnumSet;
import java.util.Objects;
import java.util.regex.Pattern;

public record PasswordConfig(
        int length,
        EnumSet<Alphabet> alphabets,
        boolean upper,
        boolean lower,
        boolean digits,
        boolean special,
        String requiredDigits
) {
    private static final Pattern ONLY_DIGITS = Pattern.compile("\\d*");

    public static PasswordConfig of(
            int length,
            EnumSet<Alphabet> alphabets,
            boolean upper,
            boolean lower,
            boolean digits,
            boolean special,
            String requiredDigits
    ) {
        EnumSet<Alphabet> safeAlphabets = (alphabets == null)
                ? EnumSet.noneOf(Alphabet.class)
                : alphabets.clone();

        String safeRequired = (requiredDigits == null) ? "" : requiredDigits.trim();

        PasswordConfig cfg = new PasswordConfig(length, safeAlphabets, upper, lower, digits, special, safeRequired);
        cfg.validate();
        return cfg;
    }

    public void validate() {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be >= 1");
        }
        if (length > 1_000_000) {
            throw new IllegalArgumentException("Length must be <= 1_000_000 (coursework range)");
        }

        Objects.requireNonNull(alphabets, "alphabets");

        if (!requiredDigits.isEmpty() && !ONLY_DIGITS.matcher(requiredDigits).matches()) {
            throw new IllegalArgumentException("requiredDigits must contain only digits 0-9");
        }
        if (!requiredDigits.isEmpty() && !digits) {
            throw new IllegalArgumentException("requiredDigits задан, но digits=false");
        }

        boolean anyLetters = !alphabets.isEmpty() && (upper || lower);
        boolean anyOther = digits || special;

        if (!anyLetters && !anyOther) {
            throw new IllegalArgumentException("No character sets selected (choose letters and/or digits/special)");
        }
    }

    public enum Alphabet {
        LATIN(
                "abcdefghijklmnopqrstuvwxyz",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        ),
        CYRILLIC(
                "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
                "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        );

        private final String lower;
        private final String upper;

        Alphabet(String lower, String upper) {
            this.lower = lower;
            this.upper = upper;
        }

        public String lower() {
            return lower;
        }

        public String upper() {
            return upper;
        }
    }
}
