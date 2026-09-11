package com.github.ruslannaumov.taskmanager.dao;

import com.github.ruslannaumov.taskmanager.model.Task;
import java.util.List;

public interface TaskDao {
    void save(Task task);
    List<Task> findAll();
}