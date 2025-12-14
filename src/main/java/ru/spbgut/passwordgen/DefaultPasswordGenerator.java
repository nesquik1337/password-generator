package ru.spbgut.passwordgen;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Реализация генератора паролей на основе SecureRandom.
 */
public final class DefaultPasswordGenerator implements PasswordGenerator {

    private static final String DIGITS = "0123456789";
    private static final String SPECIALS = "!@#$%^&*()-_=+[]{};:,.?/\\|";

    private final SecureRandom random;

    /**
     * Создаёт генератор с SecureRandom.
     */
    public DefaultPasswordGenerator() {
        this.random = new SecureRandom();
    }

    /**
     * Генерирует пароль по конфигурации.
     *
     * @param config конфигурация генерации
     * @return пароль
     */
    @Override
    public String generate(PasswordConfig config) {
        config.validate();

        Pools pools = buildPools(config);
        List<Character> mandatory = buildMandatory(config, pools);

        if (mandatory.size() > config.length()) {
            throw new IllegalArgumentException("Too many mandatory constraints for length=" + config.length());
        }

        char[] result = new char[config.length()];
        int index = putMandatory(result, mandatory);
        fillRemaining(result, index, pools.all);
        shuffle(result);

        return new String(result);
    }

    /**
     * Формирует допустимые наборы символов.
     *
     * @param config конфигурация
     * @return пулы символов
     */
    private Pools buildPools(PasswordConfig config) {
        String lowerLetters = collectLetters(config.alphabets(), true);
        String upperLetters = collectLetters(config.alphabets(), false);

        StringBuilder letters = new StringBuilder();
        if (config.lower()) letters.append(lowerLetters);
        if (config.upper()) letters.append(upperLetters);

        StringBuilder all = new StringBuilder();
        if (!letters.isEmpty()) all.append(letters);
        if (config.digits()) all.append(DIGITS);
        if (config.special()) all.append(SPECIALS);

        if (all.isEmpty()) {
            throw new IllegalArgumentException("No allowed characters after applying config");
        }

        return new Pools(lowerLetters, upperLetters, all.toString());
    }

    /**
     * Собирает буквы выбранных алфавитов с использованием StreamAPI.
     *
     * @param alphabets алфавиты
     * @param lower true — строчные, false — заглавные
     * @return строка букв
     */
    private String collectLetters(EnumSet<PasswordConfig.Alphabet> alphabets, boolean lower) {
        return alphabets.stream()
                .map(a -> lower ? a.lower() : a.upper())
                .collect(java.util.stream.Collectors.joining());
    }

    /**
     * Формирует список обязательных символов, чтобы требования точно выполнялись.
     *
     * @param config конфигурация
     * @param pools пулы символов
     * @return обязательные символы
     */
    private List<Character> buildMandatory(PasswordConfig config, Pools pools) {
        List<Character> mandatory = new ArrayList<>();
        addRequiredDigits(mandatory, config.requiredDigits());
        ensureDigitIfNeeded(mandatory, config.digits());
        ensureSpecialIfNeeded(mandatory, config.special());
        ensureCaseLetters(mandatory, config, pools);
        ensureEachAlphabet(mandatory, config);
        return mandatory;
    }

    /**
     * Добавляет обязательные цифры из requiredDigits.
     *
     * @param mandatory список обязательных символов
     * @param requiredDigits строка цифр
     */
    private void addRequiredDigits(List<Character> mandatory, String requiredDigits) {
        if (requiredDigits == null || requiredDigits.isEmpty()) return;
        for (char c : requiredDigits.toCharArray()) {
            mandatory.add(c);
        }
    }

    /**
     * Гарантирует наличие хотя бы одной цифры, если цифры включены.
     *
     * @param mandatory список обязательных символов
     * @param digitsEnabled включены ли цифры
     */
    private void ensureDigitIfNeeded(List<Character> mandatory, boolean digitsEnabled) {
        if (!digitsEnabled) return;
        if (containsDigit(mandatory)) return;
        mandatory.add(randomChar(DIGITS));
    }

    /**
     * Гарантирует наличие хотя бы одного спецсимвола, если спецсимволы включены.
     *
     * @param mandatory список обязательных символов
     * @param specialEnabled включены ли спецсимволы
     */
    private void ensureSpecialIfNeeded(List<Character> mandatory, boolean specialEnabled) {
        if (!specialEnabled) return;
        mandatory.add(randomChar(SPECIALS));
    }

    /**
     * Гарантирует наличие букв нужных регистров (если включены).
     *
     * @param mandatory список обязательных символов
     * @param config конфигурация
     * @param pools пулы символов
     */
    private void ensureCaseLetters(List<Character> mandatory, PasswordConfig config, Pools pools) {
        if (config.lower() && !pools.lowerLetters.isEmpty()) {
            mandatory.add(randomChar(pools.lowerLetters));
        }
        if (config.upper() && !pools.upperLetters.isEmpty()) {
            mandatory.add(randomChar(pools.upperLetters));
        }
    }

    /**
     * Гарантирует наличие хотя бы одной буквы из каждого выбранного алфавита.
     *
     * @param mandatory список обязательных символов
     * @param config конфигурация
     */
    private void ensureEachAlphabet(List<Character> mandatory, PasswordConfig config) {
        if (config.alphabets().isEmpty()) return;
        if (!config.lower() && !config.upper()) return;

        for (PasswordConfig.Alphabet a : config.alphabets()) {
            String local = "";
            if (config.lower()) local += a.lower();
            if (config.upper()) local += a.upper();
            if (!local.isEmpty()) {
                mandatory.add(randomChar(local));
            }
        }
    }

    /**
     * Записывает обязательные символы в начало массива результата.
     *
     * @param arr массив результата
     * @param mandatory обязательные символы
     * @return индекс, с которого продолжать заполнение
     */
    private int putMandatory(char[] arr, List<Character> mandatory) {
        int i = 0;
        for (char c : mandatory) {
            arr[i++] = c;
        }
        return i;
    }

    /**
     * Заполняет оставшиеся позиции случайными символами.
     *
     * @param arr массив результата
     * @param startIndex индекс начала заполнения
     * @param allowed допустимые символы
     */
    private void fillRemaining(char[] arr, int startIndex, String allowed) {
        for (int i = startIndex; i < arr.length; i++) {
            arr[i] = randomChar(allowed);
        }
    }

    /**
     * Перемешивает массив символов (алгоритм Фишера—Йетса).
     *
     * @param arr массив результата
     */
    private void shuffle(char[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    /**
     * Проверяет, есть ли цифра в списке обязательных символов.
     *
     * @param mandatory список обязательных символов
     * @return true, если есть цифра
     */
    private boolean containsDigit(List<Character> mandatory) {
        for (Character c : mandatory) {
            if (c != null && c >= '0' && c <= '9') return true;
        }
        return false;
    }

    /**
     * Возвращает случайный символ из заданного набора.
     *
     * @param pool набор допустимых символов
     * @return случайный символ
     */
    private char randomChar(String pool) {
        int i = random.nextInt(pool.length());
        return pool.charAt(i);
    }

    /**
     * Контейнер пулов символов.
     */
    private static final class Pools {
        private final String lowerLetters;
        private final String upperLetters;
        private final String all;

        /**
         * Создаёт пулы символов.
         *
         * @param lowerLetters строчные буквы
         * @param upperLetters заглавные буквы
         * @param all все допустимые символы
         */
        private Pools(String lowerLetters, String upperLetters, String all) {
            this.lowerLetters = lowerLetters;
            this.upperLetters = upperLetters;
            this.all = all;
        }
    }
}