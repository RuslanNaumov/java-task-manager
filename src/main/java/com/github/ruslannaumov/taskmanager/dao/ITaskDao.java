package com.github.ruslannaumov.taskmanager.dao;

import com.github.ruslannaumov.taskmanager.model.Task;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ITaskDao {
    void save(Task task);
    List<Task> findAll();
    Optional<Task> findById(Long id);
    void update(Task task);
    void deleteById(Long id);
    List<Task> findAllWithPagination(int page, int size);
    List<Task> search(String query);
    void checkConnection() throws SQLException;
}