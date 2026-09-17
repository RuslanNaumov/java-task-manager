package com.github.ruslannaumov.taskmanager.exception;

/**
 * Исключение для критических ошибок базы данных.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}