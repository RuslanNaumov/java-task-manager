package com.github.ruslannaumov.taskmanager.config;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
/**
 * Класс для управления конфигурацией приложения.
 * Хранит пути к файлам и папкам приложения.
 */
public final class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    // === Константы ===
    private static final String APP_DIR_NAME = ".java-task-manager";
    private static final String DB_FILE_NAME = "tasks.db";
    private static final String LOGS_DIR_NAME = "logs";
    private static final String CONFIG_FILE_NAME = "app.properties";

    // === Пути ===
    private static final Path APP_DIR;
    private static final Path DB_PATH;
    private static final Path LOGS_DIR;
    private static final Path CONFIG_PATH;

    // === Статический инициализатор ===
    static {
        // Получаем домашнюю папку пользователя
        String userHome = System.getProperty("user.home");

        // Создаем пути
        APP_DIR = Paths.get(userHome, APP_DIR_NAME);
        DB_PATH = APP_DIR.resolve(DB_FILE_NAME);
        LOGS_DIR = APP_DIR.resolve(LOGS_DIR_NAME);
        CONFIG_PATH = APP_DIR.resolve(CONFIG_FILE_NAME);

        // Создаем папки, если их нет
        initializeDirectories();
    }

    /**
     * Приватный конструктор, чтобы никто не мог создать объект этого класса.
     */
    private AppConfig() {
        // Пусто
    }

    /**
     * Создает необходимые папки для работы приложения.
     */
    private static void initializeDirectories() {
        try {
            if (!Files.exists(APP_DIR)) {
                Files.createDirectories(APP_DIR);
                logger.info("Created app directory: {}", APP_DIR);
            }
            if (!Files.exists(LOGS_DIR)) {
                Files.createDirectories(LOGS_DIR);
                logger.info("Created logs directory: {}", LOGS_DIR);
            }
        } catch (IOException e) {
            // Если не можем создать папки, это критическая ошибка
            logger.error("Failed to create application directories", e);
            throw new RuntimeException("Cannot initialize application directories", e);
        }
    }

    /**
     * Полностью удаляет директорию приложения и все её содержимое.
     * Используется для функции "Wipe / Reset".
     */
    public static boolean deleteAppDirectory() {
        try {
            // 1. КРИТИЧЕСКИ ВАЖНО: Останавливаем логгер, чтобы он отпустил файл app.log
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();

            // 2. Небольшая пауза (100 мс), чтобы гарантировать, что ОС Windows
            // успела освободить файловый дескриптор перед попыткой удаления.
            Thread.sleep(100);

            if (Files.exists(APP_DIR)) {
                logger.info("Starting deletion of the ENTIRE application folder: {}", APP_DIR);

                Files.walk(APP_DIR)
                        .sorted(Comparator.reverseOrder()) // Сначала файлы, потом папки, в конце - корень
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                // Теперь эта ошибка маловероятна, но оставим для безопасности
                                System.err.println("Failed to delete: " + path);
                            }
                        });

                // Проверка: действительно ли папка исчезла
                if (!Files.exists(APP_DIR)) {
                    System.out.println("SUCCESS: The entire application folder has been deleted.");
                    return true;
                } else {
                    System.out.println("WARNING: Folder still exists after deletion attempt.");
                    return false;
                }
            }
            return true; // Если папки и так нет, считаем это успехом

        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to delete application directory: " + e.getMessage());
            return false;
        }
    }

    // === Геттеры ===

    public static Path getAppDir() {
        return APP_DIR;
    }

    public static Path getDbPath() {
        return DB_PATH;
    }

    public static Path getLogsDir() {
        return LOGS_DIR;
    }

    public static Path getConfigPath() {
        return CONFIG_PATH;
    }

    /**
     * Возвращает URL для подключения к базе данных SQLite.
     */
    public static String getDbUrl() {
        return "jdbc:sqlite:" + DB_PATH;
    }

    /**
     * Проверяет, запущено ли приложение впервые (нет файла БД).
     */
    public static boolean isFirstRun() {
        return !Files.exists(DB_PATH);
    }
}