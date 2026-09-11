package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class TaskService implements ITaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private final ITaskDao taskDao;

    public TaskService(ITaskDao taskDao) {
        this.taskDao = taskDao;
    }

    @Override
    public Task createTask(String title, String description) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Заголовок задачи не может быть пустым!");
        }

        Task task = new Task(title, description);
        taskDao.save(task);
        logger.info("Сервис создал задачу: {}", title);
        return task;
    }

    @Override
    public List<Task> getAllTasks() {
        return taskDao.findAll();
    }

    @Override
    public void completeTask(Long taskId) {
        Optional<Task> optionalTask = taskDao.findById(taskId);

        if (optionalTask.isPresent()) {
            Task task = optionalTask.get();
            task.setStatus(TaskStatus.DONE);
            taskDao.update(task);
            logger.info("Сервис завершил задачу с ID: {}", taskId);
        } else {
            logger.warn("Задача с ID {} не найдена для завершения.", taskId);
        }
    }
}