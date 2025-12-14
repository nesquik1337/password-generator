package ru.spbgut.passwordgen;

/**
 * Интерфейс генератора паролей.
 */
public interface PasswordGenerator {

    /**
     * Генерирует пароль по заданной конфигурации.
     *
     * @param config конфигурация генерации
     * @return сгенерированный пароль
     */
    String generate(PasswordConfig config);
}
