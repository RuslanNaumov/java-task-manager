package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.model.Task;
import java.util.List;

public interface ITaskService {
    Task createTask(String title, String description);
    List<Task> getAllTasks();
    void completeTask(Long taskId);
}