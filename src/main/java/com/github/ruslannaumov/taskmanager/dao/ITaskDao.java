package com.github.ruslannaumov.taskmanager.dao;

import com.github.ruslannaumov.taskmanager.model.Task;
import java.util.List;
import java.util.Optional;

public interface ITaskDao {
    void save(Task task);
    List<Task> findAll();
    Optional<Task> findById(Long id);
    void update(Task task);
}