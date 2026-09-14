package com.github.ruslannaumov.taskmanager.ui;

import com.github.ruslannaumov.taskmanager.model.Task;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import com.github.ruslannaumov.taskmanager.service.ITaskService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class ConsoleUI {

    private final ITaskService taskService;
    private final Scanner scanner;
    private static final int PAGE_SIZE = 5;
    // === КОНСТАНТЫ ШИРИНЫ КОЛОНОК ===
    private static final int COL_ID = 5;
    private static final int COL_TITLE = 30;
    private static final int COL_DESC = 40;
    private static final int COL_STATUS = 15;

    public ConsoleUI(ITaskService taskService) {
        this.taskService = taskService;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        boolean running = true;

        while (running) {
            clearScreen();
            printMainMenu();

            int choice = readInt("Select option: ");

            switch (choice) {
                case 1 -> createTask();
                case 2 -> editTask();
                case 3 -> deleteTask();
                case 4 -> {
                    running = false;
                    System.out.println("Goodbye!");
                }
                default -> {
                    System.out.println("Invalid option.");
                }
            }
        }
    }

    private Long viewTask(String actionHint) {
        int currentPage = 1;

        while (true) {
            clearScreen();

            int totalPages = taskService.getTotalPages(PAGE_SIZE);
            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;

            List<Task> tasks = taskService.getTasksByPage(currentPage, PAGE_SIZE);

            // Шапка таблицы
            System.out.println("=".repeat(COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10));
            System.out.printf("%" + COL_ID + "s | %-" + COL_TITLE + "s | %-" + COL_DESC + "s | %s%n",
                    "ID", "TITLE", "DESCRIPTION", "STATUS");
            System.out.println("=".repeat(COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10));

            if (tasks.isEmpty()) {
                System.out.println("No tasks found.");
            } else {
                for (Task task : tasks) {
                    printTaskRow(task);
                }
            }

            System.out.printf("Page %d of %d%n", currentPage, totalPages);
            System.out.println("[N]ext | [P]rev | [B]ack");
            System.out.println(actionHint);
            System.out.print("> ");

            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("B")) return null;
            if (input.equalsIgnoreCase("N")) { if (currentPage < totalPages) currentPage++; continue; }
            if (input.equalsIgnoreCase("P")) { if (currentPage > 1) currentPage--; continue; }

            try {
                long id = Long.parseLong(input);
                if (taskService.getTaskById(id).isPresent()) {
                    return id;
                } else {
                    System.out.println("Task with ID " + id + " not found. Try again.");
                }
            } catch (NumberFormatException e) {
                // Игнорируем
            }
        }
    }

    private void editTask() {
        Long id = viewTask("OR Enter Task ID to edit");
        if (id != null) {
            Optional<Task> optionalTask = taskService.getTaskById(id);

            if (optionalTask.isPresent()) {
                Task task = optionalTask.get();

                clearScreen();
                System.out.println("--- EDIT TASK #" + id + " ---");
                System.out.println("Title: " + task.getTitle());
                System.out.println("Description: " + task.getDescription());
                System.out.println("Status: " + task.getStatus());
                System.out.print("New Title (or Enter to skip): ");
                String title = scanner.nextLine();
                if (title.isBlank()) title = task.getTitle();

                System.out.print("New Description (or Enter to skip): ");
                String description = scanner.nextLine();
                if (description.isBlank()) description = task.getDescription();

                System.out.print("New Status (PENDING, ACTIVE, DONE, CANCEL) (or Enter to skip): ");
                String statusInput = scanner.nextLine();
                TaskStatus status = task.getStatus();
                if (!statusInput.isBlank()) {
                    try {
                        status = TaskStatus.valueOf(statusInput.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.out.println("Invalid status, keeping old one.");
                    }
                }

                taskService.updateTask(id, title, description, status);
                System.out.println("Task updated!");
            }
        }
    }

    private void deleteTask() {
        Long id = viewTask("OR Enter Task ID to delete");

        if (id != null) {
            System.out.print("Are you sure you want to delete task #" + id + "? (y/n): ");
            String confirm = scanner.nextLine();
            if (confirm.equalsIgnoreCase("y")) {
                taskService.deleteTask(id);
                System.out.println("Task deleted!");
            } else {
                System.out.println("Deletion cancelled.");
            }
        }
    }

    private void createTask() {
        clearScreen();
        System.out.println("--- CREATE TASK ---");
        System.out.print("Title: ");
        String title = scanner.nextLine();
        System.out.print("Description: ");
        String description = scanner.nextLine();

        taskService.createTask(title, description);
        System.out.println("Task created successfully!");
    }

    /**
     * Разбивает длинный текст на строки заданной ширины.
     */
    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }

        // Если текст влезает целиком
        if (text.length() <= width) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // Если одно слово длиннее всей колонки, режем его
            while (word.length() > width) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }

            // Пытаемся добавить слово в текущую строку
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= width) {
                currentLine.append(" ").append(word);
            } else {
                // Места нет, начинаем новую строку
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private void printTaskRow(Task task) {
        List<String> titleLines = wrapText(task.getTitle(), COL_TITLE);
        List<String> descLines = wrapText(task.getDescription(), COL_DESC);

        // Количество строк, которое займет эта задача
        int linesCount = Math.max(titleLines.size(), descLines.size());
        if (linesCount == 0) linesCount = 1; // Если все поля пустые

        for (int i = 0; i < linesCount; i++) {
            // ID выводим только на первой строке
            String idStr = (i == 0) ? String.format("%" + COL_ID + "d", task.getId()) : " ".repeat(COL_ID);

            // Title
            String titleStr = (i < titleLines.size())
                    ? String.format("%-" + COL_TITLE + "s", titleLines.get(i))
                    : " ".repeat(COL_TITLE);

            // Description
            String descStr = (i < descLines.size())
                    ? String.format("%-" + COL_DESC + "s", descLines.get(i))
                    : " ".repeat(COL_DESC);

            // Status выводим только на первой строке
            String statusStr = (i == 0) ? task.getStatus().toString() : "";

            System.out.printf("%s | %s | %s | %s%n", idStr, titleStr, descStr, statusStr);
        }
        System.out.println("-".repeat(COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10));
    }

    private void printMainMenu() {
        System.out.println("");
        System.out.println("=== TASK MANAGER ===");
        System.out.println("1. ➕ Add Task");
        System.out.println("2. 📋 View Tasks");
        System.out.println("3. 🗑️ Delete Task");
        System.out.println("4. 🚪 Exit");
        System.out.println("====================");
        System.out.println("");
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}