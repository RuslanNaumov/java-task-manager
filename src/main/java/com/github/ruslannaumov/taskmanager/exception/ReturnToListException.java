package com.github.ruslannaumov.taskmanager.exception;

/**
 * Исключение для возврата к списку задач из любого режима (View/Edit/Delete/Create).
 */
public class ReturnToListException extends RuntimeException {
    public ReturnToListException() {
        super("Return to task list");
    }
}