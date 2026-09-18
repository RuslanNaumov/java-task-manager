package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.dto.TaskRequestDTO;
import com.github.ruslannaumov.taskmanager.dto.TaskResponseDTO;
import com.github.ruslannaumov.taskmanager.exception.DatabaseException;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TaskService implements ITaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);
    private final ITaskDao taskDao;

    public TaskService(ITaskDao taskDao) {
        this.taskDao = taskDao;
    }

    @Override
    public void createTask(TaskRequestDTO requestDTO) {
        // 1. Валидация на уровне сервиса
        ValidationUtils.validateTitle(requestDTO.title());
        ValidationUtils.validateDescription(requestDTO.description());
        ValidationUtils.validateStatus(requestDTO.status());

        // 2. Маппинг
        Task task = new Task();
        task.setTitle(requestDTO.title());
        task.setDescription(requestDTO.description());
        task.setStatus(requestDTO.status());
        // 3. Сохранение через DAO
        taskDao.save(task);
    }

    @Override
    public List<TaskResponseDTO> getAllTasks() {
        return taskDao.findAll().stream()
                .map(TaskResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TaskResponseDTO> getTaskById(Long id) {
        return taskDao.findById(id).map(TaskResponseDTO::from);
    }

    @Override
    public void updateTask(Long id, TaskRequestDTO dto) {
        Task existingTask = taskDao.findById(id).orElseThrow(() ->
                new RuntimeException("Task not found with id: " + id)
        );

        ValidationUtils.validateTitle(dto.title());
        ValidationUtils.validateDescription(dto.description());
        ValidationUtils.validateStatus(dto.status());

        // Обновляем поля (сеттеры сами обновят updatedAt)
        existingTask.setTitle(dto.title());
        existingTask.setDescription(dto.description());
        existingTask.setStatus(dto.status());

        taskDao.update(existingTask);
    }

    @Override
    public void deleteTask(Long id) {
        taskDao.deleteById(id);
    }

    @Override
    public int getTotalPages(int count, int size) {
        return Math.max(1, (int) Math.ceil((double) count / size));
    }

    @Override
    public List<TaskResponseDTO> searchTasks(String query) {
        return taskDao.search(query).stream()
                .map(TaskResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public void checkDatabaseConnection() throws DatabaseException {
        try {
            taskDao.checkConnection();
        } catch (SQLException e) {
            // Оборачиваем SQL-ошибку в нашу DatabaseException
            throw new DatabaseException("Cannot connect to database: " + e.getMessage(), e);
        }
    }
}