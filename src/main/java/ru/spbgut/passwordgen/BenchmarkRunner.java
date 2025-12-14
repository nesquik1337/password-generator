package ru.spbgut.passwordgen;

import java.util.EnumSet;

/**
 * Запуск замеров времени генерации паролей.
 */
public final class BenchmarkRunner {

    /**
     * Запрещает создание экземпляров класса.
     */
    private BenchmarkRunner() {
    }

    /**
     * Запускает бенчмарк и возвращает таблицу результатов.
     *
     * @param generator генератор паролей
     * @return таблица результатов
     */
    public static String run(PasswordGenerator generator) {
        int[] lengths = {10_000, 100_000, 500_000, 1_000_000};
        Profile[] profiles = {Profile.SIMPLE, Profile.MEDIUM, Profile.HARD};

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-7s | %-9s | %-10s%n", "LEVEL", "LENGTH", "AVG(ms)"));
        sb.append("-".repeat(34)).append('\n');

        for (Profile p : profiles) {
            for (int len : lengths) {
                PasswordConfig cfg = p.config(len);
                warmup(generator, cfg, 1);
                double avg = measureAvg(generator, cfg, 3);
                sb.append(String.format("%-7s | %-9d | %-10.3f%n", p.name(), len, avg));
            }
        }

        return sb.toString();
    }

    /**
     * Прогрев перед замерами.
     *
     * @param generator генератор
     * @param cfg конфигурация
     * @param times число прогревочных запусков
     */
    private static void warmup(PasswordGenerator generator, PasswordConfig cfg, int times) {
        for (int i = 0; i < times; i++) {
            generator.generate(cfg);
        }
    }

    /**
     * Возвращает среднее время по нескольким запускам.
     *
     * @param generator генератор
     * @param cfg конфигурация
     * @param repeats число повторов
     * @return среднее время в миллисекундах
     */
    private static double measureAvg(PasswordGenerator generator, PasswordConfig cfg, int repeats) {
        double sum = 0.0;
        for (int i = 0; i < repeats; i++) {
            long start = System.nanoTime();
            generator.generate(cfg);
            long end = System.nanoTime();
            sum += (end - start) / 1_000_000.0;
        }
        return sum / repeats;
    }

    /**
     * Уровни сложности для бенчмарка.
     */
    private enum Profile {
        /**
         * Минимальная сложность: латиница (нижний регистр).
         */
        SIMPLE,
        /**
         * Средняя сложность: латиница (верхний+нижний регистр) + цифры.
         */
        MEDIUM,
        /**
         * Высокая сложность: латиница+кириллица, оба регистра, цифры, спецсимволы, обязательные цифры.
         */
        HARD;

        /**
         * Создаёт конфигурацию по уровню сложности.
         *
         * @param length длина пароля
         * @return конфигурация
         */
        private PasswordConfig config(int length) {
            return switch (this) {
                case SIMPLE -> PasswordConfig.of(
                        length,
                        EnumSet.of(PasswordConfig.Alphabet.LATIN),
                        false, true,
                        false, false,
                        ""
                );
                case MEDIUM -> PasswordConfig.of(
                        length,
                        EnumSet.of(PasswordConfig.Alphabet.LATIN),
                        true, true,
                        true, false,
                        ""
                );
                case HARD -> PasswordConfig.of(
                        length,
                        EnumSet.of(PasswordConfig.Alphabet.LATIN, PasswordConfig.Alphabet.CYRILLIC),
                        true, true,
                        true, true,
                        "13579"
                );
            };
        }
    }
}