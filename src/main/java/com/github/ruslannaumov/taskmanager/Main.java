package com.github.ruslannaumov.taskmanager;

import com.github.ruslannaumov.taskmanager.config.AppConfig;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Java Task Manager is starting...");
        logger.info("App directory: {}", AppConfig.getAppDir());
        logger.info("Is first run? {}", AppConfig.isFirstRun());

        // Создаем тестовые задачи
        Task task1 = new Task("Изучить Java Core");
        task1.setId(1L); // Имитируем, что задача сохранена в БД с ID 1

        Task task2 = new Task("Написать диплом", "Сдать до 1 июня");
        task2.setStatus(TaskStatus.IN_PROGRESS); // Меняем статус

        logger.info("Created tasks:");
        logger.info("Task 1: {}", task1);
        logger.info("Task 2: {}", task2);

        logger.info("Application finished successfully");
    }
}