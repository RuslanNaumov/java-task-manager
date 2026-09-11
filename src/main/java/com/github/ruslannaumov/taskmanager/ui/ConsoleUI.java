package com.github.ruslannaumov.taskmanager.ui;

import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import com.github.ruslannaumov.taskmanager.service.ITaskService;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleUI {

    private final ITaskService taskService;
    private final Scanner scanner;

    public ConsoleUI(ITaskService taskService) {
        this.taskService = taskService;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;
        System.out.println("Welcome to Java Task Manager!");

        while (running) {
            printMenu();
            int choice = readInt("Select option: ");

            switch (choice) {
                case 1 -> createTask();
                case 2 -> listTasks();
                case 3 -> updateTask();
                case 4 -> deleteTask();
                case 5 -> {
                    running = false;
                    System.out.println("Goodbye!");
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. ➕ Add Task");
        System.out.println("2. 📋 List Tasks");
        System.out.println("3. 🔄 Update Task");
        System.out.println("4. 🗑️ Delete Task");
        System.out.println("5. 🚪 Exit");
        System.out.println("-----------------");
    }

    private void createTask() {
        System.out.print("Enter title: ");
        String title = scanner.nextLine();
        System.out.print("Enter description: ");
        String description = scanner.nextLine();

        taskService.createTask(title, description);
        System.out.println("Task created!");
    }

    private void listTasks() {
        List<Task> tasks = taskService.getAllTasks();
        if (tasks.isEmpty()) {
            System.out.println("No tasks found.");
            return;
        }

        System.out.println("\n--- TASKS ---");
        for (Task task : tasks) {
            System.out.printf("[%d] %s | Status: %s | Desc: %s%n",
                    task.getId(),
                    task.getTitle(),
                    task.getStatus(),
                    task.getDescription());
        }
    }

    private void updateTask() {
        listTasks();

        Long id = readLong("Enter Task ID to update: ");
        if (id == -1L) {
            System.out.println("Invalid ID format.");
            return;
        }

        // 1. Получаем текущую задачу из сервиса
        Optional<Task> taskOptional = taskService.getTaskById(id);

        if (taskOptional.isEmpty()) {
            System.out.println("Task with ID " + id + " not found.");
            return;
        }

        Task existingTask = taskOptional.get();

        // 2. Запрашиваем новые значения, предлагая текущие по умолчанию
        System.out.println("\n--- Updating Task ID: " + id + " ---");

        System.out.println("Current title: [" + existingTask.getTitle() + "]");
        System.out.print("Enter new title (or press Enter to keep current): ");
        String newTitle = scanner.nextLine().trim();
        if (newTitle.isBlank()) {
            newTitle = existingTask.getTitle(); // Оставляем старое значение
        }

        System.out.println("Current description: [" + existingTask.getDescription() + "]");
        System.out.print("Enter new description (or press Enter to keep current): ");
        String newDesc = scanner.nextLine().trim();
        if (newDesc.isBlank()) {
            newDesc = existingTask.getDescription(); // Оставляем старое значение
        }

        System.out.println("Current status: [" + existingTask.getStatus() + "]");
        System.out.print("Enter new status (or press Enter to keep current): ");
        String statusInput = scanner.nextLine().trim();

        TaskStatus newStatus = existingTask.getStatus(); // По умолчанию оставляем старый статус

        if (!statusInput.isBlank()) {
            try {
                newStatus = TaskStatus.valueOf(statusInput.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Invalid status format. Keeping current status: " + existingTask.getStatus());
            }
        }

        // 3. Отправляем обновленные данные в сервис
        taskService.updateTask(id, newTitle, newDesc, newStatus);
        System.out.println("Task updated successfully!");
    }

    private void deleteTask() {
        listTasks();
        Long id = readLong("Enter Task ID to delete: ");
        if (id == -1L) {
            System.out.println("Invalid ID format.");
            return;
        }
        taskService.deleteTask(id);
        System.out.println("Task deleted!");
    }

    // Вспомогательный метод для чтения Int
    private int readInt(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // Вспомогательный метод для чтения Long
    private Long readLong(String prompt) {
        System.out.print(prompt);
        try {
            return Long.parseLong(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}