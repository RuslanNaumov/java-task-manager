package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.exception.ValidationException;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import com.github.ruslannaumov.taskmanager.util.ValidationUtils;
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
        ValidationUtils.validateTitle(title);
        ValidationUtils.validateDescription(description);

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
        // 1. Валидируем входящие данные
        ValidationUtils.validateTitle(title);
        ValidationUtils.validateDescription(description);
        ValidationUtils.validateStatus(status);

        // 2. Ищем задачу
        Optional<Task> taskOptional = taskDao.findById(id);
        if (taskOptional.isEmpty()) {
            throw new ValidationException("Задача с ID " + id + " не найдена.");
        }

        // 3. Обновляем и сохраняем
        Task task = taskOptional.get();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);

        taskDao.update(task);
        logger.info("Сервис обновил задачу ID: {}", id);
    }

    @Override
    public void deleteTask(Long id) {
        taskDao.deleteById(id);
    }

    @Override
    public List<Task> getTasksByPage(int page, int size) {
        return taskDao.findAllWithPagination(page, size);
    }

    @Override
    public int getTotalPages(int size) {
        int totalTasks = taskDao.count();
        if (totalTasks == 0) return 1;
        return (int) Math.ceil((double) totalTasks / size);
    }

    @Override
    public List<Task> searchTasks(String query) {
        return taskDao.search(query);
    }
}