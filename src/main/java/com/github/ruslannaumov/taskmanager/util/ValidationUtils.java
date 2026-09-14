package com.github.ruslannaumov.taskmanager.util;

import com.github.ruslannaumov.taskmanager.exception.ValidationException;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;

public class ValidationUtils {

    public static void validateTitle(String title) {
        if (title == null || title.trim().length() < 2) {
            throw new ValidationException("Заголовок должен содержать минимум 2 символа.");
        }
        if (title.trim().length() > 50) {
            throw new ValidationException("Заголовок не может быть длиннее 50 символов.");
        }
    }

    public static void validateDescription(String description) {
        if (description == null || description.trim().length() < 2) {
            throw new ValidationException("Description должен содержать минимум 2 символа.");
        }
        if (description.length() > 100) {
            throw new ValidationException("Description не может быть длиннее 100 символов.");
        }
    }

    public static void validateStatus(TaskStatus status) {
        if (status == null) {
            throw new ValidationException("Статус задачи обязателен.");
        }
    }
}