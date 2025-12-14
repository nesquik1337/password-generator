package ru.spbgut.passwordgen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

/**
 * Точка входа консольного приложения.
 */
public final class Main {

    private static final Logger log = LogManager.getLogger(Main.class);

    private Main() {
    }

    /**
     * Запускает приложение.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0 || hasFlag(args, "--help")) {
                printHelp();
                return;
            }

            PasswordGenerator generator = new DefaultPasswordGenerator();

            if (hasFlag(args, "--benchmark")) {
                log.info("Benchmark started");
                System.out.println(BenchmarkRunner.run(generator));
                log.info("Benchmark finished");
                return;
            }

            PasswordConfig config = parseConfig(args);
            Path out = getPathArg(args, "--out");

            long start = System.nanoTime();
            String password = generator.generate(config);
            long end = System.nanoTime();

            double ms = (end - start) / 1_000_000.0;
            log.info("Generated password length={}, timeMs={}", config.length(), String.format("%.3f", ms));

            if (out != null) {
                writeToFile(out, password);
                System.out.println("Saved to: " + out.toAbsolutePath());
                System.out.println("Time (ms): " + String.format("%.3f", ms));
                return;
            }

            printPasswordPreview(password, config.length());
            System.out.println("Time (ms): " + String.format("%.3f", ms));

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage(), e);
            System.out.println("Error: " + e.getMessage());
            System.out.println("Use --help");
        }
    }

    /**
     * Создаёт конфигурацию из аргументов командной строки.
     *
     * @param args аргументы
     * @return конфигурация
     */
    private static PasswordConfig parseConfig(String[] args) {
        int length = getIntArg(args, "--length", 16);
        EnumSet<PasswordConfig.Alphabet> alphabets = parseAlphabets(getStringArg(args, "--alphabets", "latin"));

        boolean upper = hasFlag(args, "--upper");
        boolean lower = hasFlag(args, "--lower");
        boolean digits = hasFlag(args, "--digits");
        boolean special = hasFlag(args, "--special");

        if (!upper && !lower) {
            lower = true;
        }

        String requiredDigits = getStringArg(args, "--requiredDigits", "");
        return PasswordConfig.of(length, alphabets, upper, lower, digits, special, requiredDigits);
    }

    /**
     * Парсит значение параметра --alphabets (например: latin,cyrillic).
     *
     * @param value строка алфавитов
     * @return набор алфавитов
     */
    private static EnumSet<PasswordConfig.Alphabet> parseAlphabets(String value) {
        EnumSet<PasswordConfig.Alphabet> set = EnumSet.noneOf(PasswordConfig.Alphabet.class);
        if (value == null || value.isBlank()) {
            set.add(PasswordConfig.Alphabet.LATIN);
            return set;
        }

        String[] parts = value.split(",");
        for (String p : parts) {
            String s = p.trim().toLowerCase();
            if (s.equals("latin")) set.add(PasswordConfig.Alphabet.LATIN);
            else if (s.equals("cyrillic")) set.add(PasswordConfig.Alphabet.CYRILLIC);
            else throw new IllegalArgumentException("Unknown alphabet: " + p);
        }

        return set;
    }

    /**
     * Выводит пароль полностью или превью для больших длин.
     *
     * @param password пароль
     * @param length длина пароля
     */
    private static void printPasswordPreview(String password, int length) {
        if (length <= 2000) {
            System.out.println(password);
            return;
        }
        System.out.println(password.substring(0, 2000));
        System.out.println("\n... (printed first 2000 chars of " + length + ")");
        System.out.println("Tip: use --out <file> to save full password.");
    }

    /**
     * Записывает строку в файл.
     *
     * @param path путь файла
     * @param text текст
     * @throws Exception ошибка записи
     */
    private static void writeToFile(Path path, String text) throws Exception {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, text);
    }

    /**
     * Проверяет наличие флага в args.
     *
     * @param args аргументы
     * @param flag флаг
     * @return true, если флаг присутствует
     */
    private static boolean hasFlag(String[] args, String flag) {
        for (String a : args) {
            if (a.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }

    /**
     * Возвращает строковый аргумент вида --key value.
     *
     * @param args аргументы
     * @param key ключ
     * @param def значение по умолчанию
     * @return значение
     */
    private static String getStringArg(String[] args, String key, String def) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(key)) return args[i + 1];
        }
        return def;
    }

    /**
     * Возвращает целочисленный аргумент вида --key value.
     *
     * @param args аргументы
     * @param key ключ
     * @param def значение по умолчанию
     * @return число
     */
    private static int getIntArg(String[] args, String key, int def) {
        String s = getStringArg(args, key, null);
        if (s == null) return def;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + key + ": " + s);
        }
    }

    /**
     * Возвращает путь из аргумента вида --key value.
     *
     * @param args аргументы
     * @param key ключ
     * @return путь или null
     */
    private static Path getPathArg(String[] args, String key) {
        String s = getStringArg(args, key, null);
        if (s == null) return null;
        return Path.of(s);
    }

    /**
     * Выводит справку по аргументам.
     */
    private static void printHelp() {
        System.out.println("""
                Password Generator (console)
                Usage:
                  --length <n>
                  --alphabets latin,cyrillic
                  --upper --lower --digits --special
                  --requiredDigits <digits>
                  --out <file>
                  --benchmark
                  --help
                """);
    }
}