package com.github.ruslannaumov.taskmanager.dao;

import com.github.ruslannaumov.taskmanager.exception.DatabaseException;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import com.github.ruslannaumov.taskmanager.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
            logger.error("Database error", e);
            throw new DatabaseException("Failed to execute database operation: " + e.getMessage(), e);
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
            logger.error("Database error", e);
            throw new DatabaseException("Failed to execute database operation: " + e.getMessage(), e);
        }
        return tasks;
    }

    @Override
    public List<Task> search(String query) {
        List<Task> tasks = new ArrayList<>();

        // 1. Защита от пустых запросов
        if (query == null || query.isBlank()) {
            return tasks;
        }

        // 2. Формируем шаблон для поиска: %запрос%
        String searchPattern = "%" + query.trim() + "%";

        // 3. SQL-запрос с оператором LIKE
        String sql = """
            SELECT id, title, description, status, created_at, updated_at
            FROM tasks
            WHERE title LIKE ? 
               OR description LIKE ? 
               OR status LIKE ?
            ORDER BY created_at ASC
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 4. Подставляем шаблон
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error during search", e);
            throw new DatabaseException("Failed to execute search operation: " + e.getMessage(), e);
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
            logger.error("Database error", e);
            throw new DatabaseException("Failed to execute database operation: " + e.getMessage(), e);
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
            logger.error("Database error", e);
            throw new DatabaseException("Failed to execute database operation: " + e.getMessage(), e);
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
            logger.error("Database error", e);
            throw new DatabaseException("Failed to execute database operation: " + e.getMessage(), e);
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
            logger.error("Database error", e);
            throw new DatabaseException("Failed to execute database operation: " + e.getMessage(), e);
        }
        return tasks;
    }

    @Override
    public void checkConnection() throws SQLException {
        // Мы проверяем не просто соединение, а существование нашей главной таблицы.
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Пытаемся прочитать 1 строку из таблицы tasks
            stmt.execute("SELECT 1 FROM tasks LIMIT 1");
        }
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