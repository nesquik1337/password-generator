package ru.spbgut.passwordgen;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class DefaultPasswordGenerator implements PasswordGenerator {

    private static final String DIGITS = "0123456789";
    private static final String SPECIALS = "!@#$%^&*()-_=+[]{};:,.?/\\|";

    private final SecureRandom random;

    public DefaultPasswordGenerator() {
        this.random = new SecureRandom();
    }

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

    private String collectLetters(EnumSet<PasswordConfig.Alphabet> alphabets, boolean lower) {
        if (alphabets.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (PasswordConfig.Alphabet a : alphabets) {
            sb.append(lower ? a.lower() : a.upper());
        }
        return sb.toString();
    }

    private List<Character> buildMandatory(PasswordConfig config, Pools pools) {
        List<Character> mandatory = new ArrayList<>();
        addRequiredDigits(mandatory, config.requiredDigits());
        ensureDigitIfNeeded(mandatory, config.digits());
        ensureSpecialIfNeeded(mandatory, config.special());
        ensureCaseLetters(mandatory, config, pools);
        ensureEachAlphabet(mandatory, config);
        return mandatory;
    }

    private void addRequiredDigits(List<Character> mandatory, String requiredDigits) {
        if (requiredDigits == null || requiredDigits.isEmpty()) return;
        for (char c : requiredDigits.toCharArray()) {
            mandatory.add(c);
        }
    }

    private void ensureDigitIfNeeded(List<Character> mandatory, boolean digitsEnabled) {
        if (!digitsEnabled) return;
        if (containsDigit(mandatory)) return;
        mandatory.add(randomChar(DIGITS));
    }

    private void ensureSpecialIfNeeded(List<Character> mandatory, boolean specialEnabled) {
        if (!specialEnabled) return;
        mandatory.add(randomChar(SPECIALS));
    }

    private void ensureCaseLetters(List<Character> mandatory, PasswordConfig config, Pools pools) {
        if (config.lower() && !pools.lowerLetters.isEmpty()) {
            mandatory.add(randomChar(pools.lowerLetters));
        }
        if (config.upper() && !pools.upperLetters.isEmpty()) {
            mandatory.add(randomChar(pools.upperLetters));
        }
    }

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

    private int putMandatory(char[] arr, List<Character> mandatory) {
        int i = 0;
        for (char c : mandatory) {
            arr[i++] = c;
        }
        return i;
    }

    private void fillRemaining(char[] arr, int startIndex, String allowed) {
        for (int i = startIndex; i < arr.length; i++) {
            arr[i] = randomChar(allowed);
        }
    }

    private void shuffle(char[] arr) {
        for (int i = arr.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
    }

    private boolean containsDigit(List<Character> mandatory) {
        for (Character c : mandatory) {
            if (c != null && c >= '0' && c <= '9') return true;
        }
        return false;
    }

    private char randomChar(String pool) {
        int i = random.nextInt(pool.length());
        return pool.charAt(i);
    }

    private static final class Pools {
        private final String lowerLetters;
        private final String upperLetters;
        private final String all;

        private Pools(String lowerLetters, String upperLetters, String all) {
            this.lowerLetters = lowerLetters;
            this.upperLetters = upperLetters;
            this.all = all;
        }
    }
}
