package ru.spbgut.passwordgen;

import java.util.EnumSet;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Конфигурация генерации пароля.
 *
 * @param length длина пароля
 * @param alphabets набор алфавитов
 * @param upper использовать заглавные буквы
 * @param lower использовать строчные буквы
 * @param digits использовать цифры
 * @param special использовать специальные символы
 * @param requiredDigits строка цифр, которые должны встретиться в пароле
 */
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

    /**
     * Создаёт конфигурацию и выполняет валидацию.
     *
     * @param length длина пароля
     * @param alphabets набор алфавитов
     * @param upper использовать заглавные буквы
     * @param lower использовать строчные буквы
     * @param digits использовать цифры
     * @param special использовать специальные символы
     * @param requiredDigits строка обязательных цифр
     * @return валидная конфигурация
     */
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

    /**
     * Проверяет параметры конфигурации.
     *
     * @throws IllegalArgumentException если параметры некорректны
     */
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

    /**
     * Поддерживаемые алфавиты.
     */
    public enum Alphabet {
        /**
         * Латинский алфавит.
         */
        LATIN(
                "abcdefghijklmnopqrstuvwxyz",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        ),
        /**
         * Кириллический алфавит.
         */
        CYRILLIC(
                "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
                "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
        );

        /**
         * Строчные буквы алфавита.
         */
        private final String lower;

        /**
         * Заглавные буквы алфавита.
         */
        private final String upper;

        /**
         * Создаёт алфавит.
         *
         * @param lower строчные буквы
         * @param upper заглавные буквы
         */
        Alphabet(String lower, String upper) {
            this.lower = lower;
            this.upper = upper;
        }

        /**
         * Возвращает строчные буквы алфавита.
         *
         * @return строка строчных букв
         */
        public String lower() {
            return lower;
        }

        /**
         * Возвращает заглавные буквы алфавита.
         *
         * @return строка заглавных букв
         */
        public String upper() {
            return upper;
        }
    }
}