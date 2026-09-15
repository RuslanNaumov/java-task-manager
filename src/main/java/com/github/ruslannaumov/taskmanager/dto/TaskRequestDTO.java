package com.github.ruslannaumov.taskmanager.dto;

import com.github.ruslannaumov.taskmanager.model.TaskStatus;

/**
 * DTO для создания или обновления задачи.
 */
public record TaskRequestDTO(
        String title,
        String description,
        TaskStatus status
) {
    /**
     * Здесь мы нормализуем данные: убираем лишние пробелы.
     */
    public TaskRequestDTO {
        if (title != null) {
            title = title.trim();
        }
        if (description != null) {
            description = description.trim();
        }
    }

    /**
     * Фабричный метод для создания задачи только с заголовком и описанием.
     */
    public static TaskRequestDTO forCreation(String title, String description) {
        return new TaskRequestDTO(title, description, null);
    }

    /**
     * Фабричный метод для обновления (позволяет передавать null, если поле не меняется).
     */
    public static TaskRequestDTO forUpdate(String title, String description, TaskStatus status) {
        return new TaskRequestDTO(title, description, status);
    }
}