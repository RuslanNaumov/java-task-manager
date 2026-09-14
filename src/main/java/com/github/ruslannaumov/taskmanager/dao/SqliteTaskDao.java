package com.github.ruslannaumov.taskmanager.dao;

import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import com.github.ruslannaumov.taskmanager.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SqliteTaskDao implements ITaskDao {
    private static final Logger logger = LoggerFactory.getLogger(SqliteTaskDao.class);

    @Override
    public void save(Task task) {
        String sql = "INSERT INTO tasks (title, description, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Подставляем значения вместо ?
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getStatus().name());
            pstmt.setString(4, task.getCreatedAt().toString());
            pstmt.setString(5, task.getUpdatedAt().toString());

            pstmt.executeUpdate();
            logger.info("Task saved to DB: {}", task.getTitle());

        } catch (SQLException e) {
            logger.error("Error saving task", e);
        }
    }

    @Override
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, title, description, status, created_at, updated_at FROM tasks";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }

        } catch (SQLException e) {
            logger.error("Error loading tasks", e);
        }
        return tasks;
    }

    public List<Task> search(String query) {
        List<Task> tasks = new ArrayList<>();

        if (query == null || query.isBlank()) {
            return tasks;
        }

        String searchText = query.trim().toLowerCase(Locale.ROOT);

        String sql = """
            SELECT id, title, description, status, created_at, updated_at
            FROM tasks
            ORDER BY created_at ASC 
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Task task = mapResultSetToTask(rs);

                String title = task.getTitle() == null
                        ? ""
                        : task.getTitle().toLowerCase(Locale.ROOT);

                String description = task.getDescription() == null
                        ? ""
                        : task.getDescription().toLowerCase(Locale.ROOT);

                String status = task.getStatus() == null
                        ? ""
                        : task.getStatus().name().toLowerCase(Locale.ROOT);

                if (title.contains(searchText)
                        || description.contains(searchText)
                        || status.contains(searchText)) {
                    tasks.add(task);
                }
            }
        } catch (SQLException e) {
            logger.error("Error searching tasks", e);
        }

        return tasks;
    }



    @Override
    public Optional<Task> findById(Long id) {
        String sql = "SELECT id, title, description, status, created_at, updated_at FROM tasks WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Error finding task by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public void update(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, status = ?, updated_at = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getStatus().name());
            pstmt.setString(4, task.getUpdatedAt().toString());
            pstmt.setLong(5, task.getId());

            pstmt.executeUpdate();
            logger.info("Task updated: {}", task.getTitle());

        } catch (SQLException e) {
            logger.error("Error updating task", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM tasks WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error deleting task", e);
        }
    }

    @Override
    public List<Task> findAllWithPagination(int page, int size) {
        List<Task> tasks = new ArrayList<>();
        // Если страница 1, размер 5 -> пропускаем 0, берем 5
        // Если страница 2, размер 5 -> пропускаем 5, берем 5
        int offset = (page - 1) * size;

        String sql = "SELECT id, title, description, status, created_at, updated_at FROM tasks LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, size);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error loading tasks with pagination", e);
        }
        return tasks;
    }

    @Override
    public int count() {
        String sql = "SELECT COUNT(*) FROM tasks";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting tasks", e);
        }
        return 0;
    }

    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        task.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        task.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at")));
        return task;
    }
}