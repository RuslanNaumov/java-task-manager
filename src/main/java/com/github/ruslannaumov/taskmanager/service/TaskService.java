package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.dto.TaskRequestDTO;
import com.github.ruslannaumov.taskmanager.dto.TaskResponseDTO;
import com.github.ruslannaumov.taskmanager.exception.ValidationException;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public TaskResponseDTO createTask(TaskRequestDTO requestDTO) {
        ValidationUtils.validateTitle(requestDTO.title());
        ValidationUtils.validateDescription(requestDTO.description());

        Task task = new Task(requestDTO.title(), requestDTO.description());
        taskDao.save(task);

        logger.info("Сервис создал задачу: {}", task.getTitle());
        return TaskResponseDTO.from(task);
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
    public TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDTO) {
        Task task = taskDao.findById(id)
                .orElseThrow(() -> new ValidationException("Задача с ID " + id + " не найдена."));

        // Применяем изменения только если новые данные не пустые
        if (requestDTO.title() != null && !requestDTO.title().isBlank()) {
            ValidationUtils.validateTitle(requestDTO.title());
            task.setTitle(requestDTO.title());
        }

        if (requestDTO.description() != null && !requestDTO.description().isBlank()) {
            ValidationUtils.validateDescription(requestDTO.description());
            task.setDescription(requestDTO.description());
        }

        if (requestDTO.status() != null) {
            ValidationUtils.validateStatus(requestDTO.status());
            task.setStatus(requestDTO.status());
        }

        taskDao.update(task);
        logger.info("Сервис обновил задачу ID: {}", id);

        return TaskResponseDTO.from(task);
    }

    @Override
    public void deleteTask(Long id) {
        taskDao.deleteById(id);
    }

    @Override
    public List<TaskResponseDTO> getTasksByPage(int page, int size) {
        return taskDao.findAllWithPagination(page, size).stream()
                .map(TaskResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Override
    public int getTotalPages(int size) {
        int totalTasks = taskDao.count();
        if (totalTasks == 0) return 1;
        return (int) Math.ceil((double) totalTasks / size);
    }

    @Override
    public List<TaskResponseDTO> searchTasks(String query) {
        return taskDao.search(query).stream()
                .map(TaskResponseDTO::from)
                .collect(Collectors.toList());
    }
}