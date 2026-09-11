package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;

import java.util.List;
import java.util.Optional;

public interface ITaskService {
    Task createTask(String title, String description);
    List<Task> getAllTasks();
    void updateTask(Long id, String title, String description, TaskStatus status);
    void deleteTask(Long id);
    Optional<Task> getTaskById(Long id);
}