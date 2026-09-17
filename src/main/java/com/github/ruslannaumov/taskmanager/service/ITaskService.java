package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.dto.TaskRequestDTO;
import com.github.ruslannaumov.taskmanager.dto.TaskResponseDTO;

import java.util.List;
import java.util.Optional;

public interface ITaskService {
    void createTask(TaskRequestDTO requestDTO);
    void updateTask(Long id, TaskRequestDTO requestDTO);
    void deleteTask(Long id);
    List<TaskResponseDTO> getAllTasks();
    List<TaskResponseDTO> getTasksByPage(int page, int size);
    List<TaskResponseDTO> searchTasks(String query);
    Optional<TaskResponseDTO> getTaskById(Long id);
    int getTotalPages(int count, int size);
    int getTaskCount();
}