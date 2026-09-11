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
    public Optional<Task> getTaskById(Long id) {
        return taskDao.findById(id);
    }

    @Override
    public void updateTask(Long id, String title, String description, TaskStatus status) {
        Optional<Task> taskOptional = taskDao.findById(id);

        if (taskOptional.isPresent()) {
            // Достаем объект из Optional
            Task task = taskOptional.get();

            task.setTitle(title);
            task.setDescription(description);
            task.setStatus(status);

            taskDao.update(task);
        } else {
            System.out.println("Task with ID " + id + " not found.");
        }
    }

    @Override
    public void deleteTask(Long id) {
        taskDao.deleteById(id);
    }
}