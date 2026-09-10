package com.github.ruslannaumov.taskmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    // Создаем логгер для этого класса
    // LoggerFactory — это "фабрика", которая создает логгеры
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // INFO: Важное событие
        logger.info("Java Task Manager is starting...");

        // DEBUG: Отладочная информация (в консоли не увидим, т.к. в конфиге level=INFO)
        logger.debug("This is a debug message - useful for developers");

        // WARN: Предупреждение
        logger.warn("Configuration file not found, using defaults");

        // ERROR: Ошибка
        logger.error("Failed to connect to database");

        // INFO: Завершение
        logger.info("Application finished successfully");
    }
}