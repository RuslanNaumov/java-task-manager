package com.github.ruslannaumov.taskmanager;

import com.github.ruslannaumov.taskmanager.config.AppConfig;
import com.github.ruslannaumov.taskmanager.database.DatabaseInitializer;
import com.github.ruslannaumov.taskmanager.dao.SqliteTaskDao;
import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Java Task Manager is starting...");
        logger.info("App directory: {}", AppConfig.getAppDir());

        // 1. Инициализация базы данных
        DatabaseInitializer.initialize();

        // 2. Настройка связей
        ITaskDao taskDao = new SqliteTaskDao();
        TaskService taskService = new TaskService(taskDao);

        // 3. Используем сервис для создания задач и сохраняем результат
        Task task1 = taskService.createTask("Изучить сервисный слой", "Понять паттерн Фасад и внедрение зависимостей");
        logger.info("Создана задача с ID: {}", task1.getId());

        Task task2 = taskService.createTask("Выпить кофе", "");
        logger.info("Создана задача с ID: {}", task2.getId());

        // 4. Получаем все задачи
        List<Task> tasks = taskService.getAllTasks();
        logger.info("Загружено задач: {}", tasks.size());

        // 5. Завершаем первую задачу (допустим, её ID = 1)
        if (!tasks.isEmpty()) {
            Long firstTaskId = tasks.getFirst().getId();
            taskService.completeTask(firstTaskId);
        }

        // 6. Проверяем результат
        List<Task> updatedTasks = taskService.getAllTasks();
        for (Task task : updatedTasks) {
            logger.info("{}", task);
        }
    }
}