package com.github.ruslannaumov.taskmanager;

import com.github.ruslannaumov.taskmanager.dao.SqliteTaskDao;
import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.database.DatabaseInitializer;
import com.github.ruslannaumov.taskmanager.exception.DatabaseException;
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

        // 1. Инициализация БД с защитой от фатальной ошибки
        try {
            DatabaseInitializer.initialize();
        } catch (DatabaseException e) {
            // Создаем UI без сервиса, чтобы показать экран ошибки
            ConsoleUI errorUI = new ConsoleUI(null);
            errorUI.displayFatalDatabaseError(e.getMessage());
            return;
        }

        // 2. Создание зависимостей
        ITaskDao taskDao = new SqliteTaskDao();
        ITaskService taskService = new TaskService(taskDao);

        // 3. Запуск UI
        ConsoleUI ui = new ConsoleUI(taskService);
        ui.start();

        // 4. Отключаем Jansi при завершении программы
        AnsiConsole.systemUninstall();
    }
}