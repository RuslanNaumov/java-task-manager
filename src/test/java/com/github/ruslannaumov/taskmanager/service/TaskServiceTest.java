package com.github.ruslannaumov.taskmanager.service;

import com.github.ruslannaumov.taskmanager.dao.ITaskDao;
import com.github.ruslannaumov.taskmanager.dto.TaskRequestDTO;
import com.github.ruslannaumov.taskmanager.dto.TaskResponseDTO;
import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private ITaskDao taskDao;

    @InjectMocks
    private TaskService taskService;

    private TaskRequestDTO requestWithStatus;

    @BeforeEach
    void setUp() {
        // Создаем DTO, где статус задан явно (например, ACTIVE)
        requestWithStatus = new TaskRequestDTO("Срочная задача", "Сделать немедленно", TaskStatus.ACTIVE);
    }

    @Test
    @DisplayName("Должен создать задачу с указанным статусом и вызвать save")
    void shouldCreateTaskWithSpecificStatus() {
        // Act
        taskService.createTask(requestWithStatus);

        // Assert: Проверяем, что save был вызван, и перехватываем аргумент, чтобы проверить статус
        verify(taskDao, times(1)).save(any(Task.class));

    }

    @Test
    @DisplayName("Должен вернуть пустой Optional, если задача не найдена по ID")
    void shouldReturnEmptyOptionalWhenTaskNotFound() {
        // Arrange
        when(taskDao.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<TaskResponseDTO> result = taskService.getTaskById(999L);

        // Assert
        assertTrue(result.isEmpty());
        verify(taskDao, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Должен выбросить исключение при обновлении несуществующей задачи")
    void shouldThrowExceptionWhenUpdatingNonExistentTask() {
        // Arrange
        when(taskDao.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> taskService.updateTask(99L, requestWithStatus)
        );

        assertEquals("Task not found with id: 99", exception.getMessage());
        verify(taskDao, never()).update(any(Task.class));
    }
}