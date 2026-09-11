package com.github.ruslannaumov.taskmanager;

import com.github.ruslannaumov.taskmanager.database.DatabaseInitializer;
import com.github.ruslannaumov.taskmanager.dao.SqliteTaskDao;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Java Task Manager is starting...");

        // 1. Инициализация базы данных
        DatabaseInitializer.initialize();

        // 2. Создаем DAO
        SqliteTaskDao taskDao = new SqliteTaskDao();

        // 3. Создаем и сохраняем задачу
        Task task = new Task("SQLite", "Connect Java to SQLite database");
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskDao.save(task);

        // 4. Читаем все задачи из базы
        var tasks = taskDao.findAll();
        logger.info("Total tasks in DB: {}", tasks.size());
        for (Task t : tasks) {
            logger.info("{}", t);
        }
    }
}