package com.github.ruslannaumov.taskmanager;

import com.github.ruslannaumov.taskmanager.dao.SqliteTaskDao;
import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.database.DatabaseInitializer;
import com.github.ruslannaumov.taskmanager.service.ITaskService;
import com.github.ruslannaumov.taskmanager.service.TaskService;
import com.github.ruslannaumov.taskmanager.ui.ConsoleUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.fusesource.jansi.AnsiConsole;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 1. Включаем JANSI
        AnsiConsole.systemInstall();

        logger.info("Java Task Manager is starting...");

        // 1. Инициализация БД
        DatabaseInitializer.initialize();

        // 2. Создание зависимостей (связываем слои)
        ITaskDao taskDao = new SqliteTaskDao();
        ITaskService taskService = new TaskService(taskDao);

        // 3. Создание и запуск UI
        ConsoleUI ui = new ConsoleUI(taskService);
        ui.start();

        // 4. Отключаем Jansi при завершении программы
        AnsiConsole.systemUninstall();
    }
}