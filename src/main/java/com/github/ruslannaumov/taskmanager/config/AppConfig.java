package com.github.ruslannaumov.taskmanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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