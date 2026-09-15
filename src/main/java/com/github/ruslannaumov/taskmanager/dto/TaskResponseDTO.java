package com.github.ruslannaumov.taskmanager.dto;

import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;

import java.time.LocalDateTime;

/**
 * DTO для отображения задачи в UI.
 * Содержит только те данные, которые нужны пользователю.
 */
public record TaskResponseDTO(
        Long id,
        String title,
        String description,
        TaskStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * Статический фабричный метод для маппинга Entity (Task) в DTO.
     * Это изолирует логику преобразования в одном месте.
     */
    public static TaskResponseDTO from(Task task) {
        return new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}