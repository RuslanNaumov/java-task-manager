package com.github.ruslannaumov.taskmanager;

import com.github.ruslannaumov.taskmanager.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Java Task Manager is starting...");

        // Обращаемся к AppConfig.
        // При первом обращении сработает статический инициализатор,
        // и папки будут созданы автоматически.
        logger.info("App directory: {}", AppConfig.getAppDir());
        logger.info("Database path: {}", AppConfig.getDbPath());
        logger.info("Database URL: {}", AppConfig.getDbUrl());
        logger.info("Is first run? {}", AppConfig.isFirstRun());

        logger.info("Configuration loaded successfully");
    }
}