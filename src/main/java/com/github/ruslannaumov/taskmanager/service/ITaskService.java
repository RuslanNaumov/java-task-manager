package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.dto.TaskRequestDTO;
import com.github.ruslannaumov.taskmanager.dto.TaskResponseDTO;

import java.util.List;
import java.util.Optional;

public interface ITaskService {
    TaskResponseDTO createTask(TaskRequestDTO requestDTO);
    List<TaskResponseDTO> getAllTasks();
    Optional<TaskResponseDTO> getTaskById(Long id);
    TaskResponseDTO updateTask(Long id, TaskRequestDTO requestDTO);
    void deleteTask(Long id);
    List<TaskResponseDTO> getTasksByPage(int page, int size);
    int getTotalPages(int size);
    List<TaskResponseDTO> searchTasks(String query);
}