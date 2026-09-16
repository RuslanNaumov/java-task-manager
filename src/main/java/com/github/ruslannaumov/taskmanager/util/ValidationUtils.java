package com.github.ruslannaumov.taskmanager.util;

import com.github.ruslannaumov.taskmanager.exception.ValidationException;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;

public class ValidationUtils {

    public static void validateTitle(String title) {
        if (title == null || title.trim().length() < 2) {
            throw new ValidationException("Title must contain at least 2 characters.");
        }
        if (title.trim().length() > 50) {
            throw new ValidationException("Title cannot be longer than 50 characters.");
        }
    }

    public static void validateDescription(String description) {
        if (description == null) {
            throw new ValidationException("Description cannot be null.");
        }
        String trimmed = description.trim();
        if (trimmed.length() < 2) {
            throw new ValidationException("Description must contain at least 2 characters.");
        }
        if (trimmed.length() > 100) {
            throw new ValidationException("Description cannot be longer than 100 characters.");
        }
    }

    public static void validateStatus(TaskStatus status) {
        if (status == null) {
            throw new ValidationException("Status required");
        }
    }
}