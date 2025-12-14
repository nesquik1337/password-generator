package ru.spbgut.passwordgen;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultPasswordGeneratorTest {

    @Test
    void generatesCorrectLength() {
        PasswordGenerator gen = new DefaultPasswordGenerator();
        PasswordConfig cfg = PasswordConfig.of(
                32,
                EnumSet.of(PasswordConfig.Alphabet.LATIN),
                true, true,
                true, true,
                "135"
        );
        String p = gen.generate(cfg);
        assertEquals(32, p.length());
    }

    @Test
    void containsRequiredDigits() {
        PasswordGenerator gen = new DefaultPasswordGenerator();
        PasswordConfig cfg = PasswordConfig.of(
                40,
                EnumSet.of(PasswordConfig.Alphabet.LATIN),
                true, true,
                true, false,
                "13579"
        );
        String p = gen.generate(cfg);

        assertTrue(p.indexOf('1') >= 0);
        assertTrue(p.indexOf('3') >= 0);
        assertTrue(p.indexOf('5') >= 0);
        assertTrue(p.indexOf('7') >= 0);
        assertTrue(p.indexOf('9') >= 0);
    }

    @Test
    void containsUpperAndLowerWhenEnabled() {
        PasswordGenerator gen = new DefaultPasswordGenerator();
        PasswordConfig cfg = PasswordConfig.of(
                30,
                EnumSet.of(PasswordConfig.Alphabet.LATIN),
                true, true,
                false, false,
                ""
        );
        String p = gen.generate(cfg);

        assertTrue(p.chars().anyMatch(ch -> ch >= 'A' && ch <= 'Z'));
        assertTrue(p.chars().anyMatch(ch -> ch >= 'a' && ch <= 'z'));
    }

    @Test
    void containsDigitWhenDigitsEnabled() {
        PasswordGenerator gen = new DefaultPasswordGenerator();
        PasswordConfig cfg = PasswordConfig.of(
                25,
                EnumSet.of(PasswordConfig.Alphabet.LATIN),
                false, true,
                true, false,
                ""
        );
        String p = gen.generate(cfg);

        assertTrue(p.chars().anyMatch(ch -> ch >= '0' && ch <= '9'));
    }

    @Test
    void containsSpecialWhenSpecialEnabled() {
        PasswordGenerator gen = new DefaultPasswordGenerator();
        PasswordConfig cfg = PasswordConfig.of(
                25,
                EnumSet.of(PasswordConfig.Alphabet.LATIN),
                false, true,
                false, true,
                ""
        );
        String p = gen.generate(cfg);

        String specials = "!@#$%^&*()-_=+[]{};:,.?/\\|";
        assertTrue(p.chars().anyMatch(ch -> specials.indexOf(ch) >= 0));
    }

    @Test
    void throwsWhenRequiredDigitsButDigitsDisabled() {
        assertThrows(IllegalArgumentException.class, () -> PasswordConfig.of(
                20,
                EnumSet.of(PasswordConfig.Alphabet.LATIN),
                false, true,
                false, false,
                "123"
        ));
    }

    @Test
    void throwsWhenLengthOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> PasswordConfig.of(
                0,
                EnumSet.of(PasswordConfig.Alphabet.LATIN),
                false, true,
                true, false,
                ""
        ));
    }
}
