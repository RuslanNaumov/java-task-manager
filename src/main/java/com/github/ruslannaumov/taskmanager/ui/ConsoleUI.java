package com.github.ruslannaumov.taskmanager.ui;

import com.github.ruslannaumov.taskmanager.config.AppConfig;
import com.github.ruslannaumov.taskmanager.dto.TaskRequestDTO;
import com.github.ruslannaumov.taskmanager.dto.TaskResponseDTO;
import com.github.ruslannaumov.taskmanager.exception.ReturnToMainMenuException;
import com.github.ruslannaumov.taskmanager.exception.ValidationException;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import com.github.ruslannaumov.taskmanager.service.ITaskService;
import com.github.ruslannaumov.taskmanager.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {

    private final ITaskService taskService;
    private final Scanner scanner;
    private static final int PAGE_SIZE = 5;

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
        clearScreen();

        while (running) {
            try {
                printMainMenu();
                int choice = readValidMenuOption();

                switch (choice) {
                    case 1 -> createTask();
                    case 2 -> viewTasks();
                    case 3 -> clearDatabaseAndExit();
                    case 4 -> {
                        running = false;
                        System.out.println("Goodbye!");
                    }
                }
            } catch (ReturnToMainMenuException e) {
                // Перехватываем исключение и просто продолжаем цикл,
                // который заново отрисует Главное Меню.
                clearScreen();
            }

            if (running) {
                clearScreen();
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n=== TASK MANAGER ===");
        System.out.println("1. ➕ Add Task");
        System.out.println("2. 📋 View Tasks");
        System.out.println("3. 🧹 Clear Database & Exit");
        System.out.println("4. 🚪 Exit");
        System.out.println("====================");
    }

    private void viewTasks() {
        String currentQuery = null;

        while (true) {
            List<TaskResponseDTO> tasks;
            if (currentQuery == null) {
                tasks = taskService.getAllTasks();
            } else {
                tasks = taskService.searchTasks(currentQuery);
                if (tasks.isEmpty()) {
                    clearScreen();
                    System.out.printf("No tasks found for query: \"%s\"%n", currentQuery);
                    String newQuery = readValidSearchQuery();
                    currentQuery = newQuery; // Если ввели M, сработает исключение
                    continue;
                }
            }

            String action = displayTaskListAndHandleActions(tasks, currentQuery);

            if (action.equals("SEARCH")) {
                clearScreen();
                String newQuery = readValidSearchQuery();
                currentQuery = newQuery;
            }
        }
    }

    private String displayTaskListAndHandleActions(List<TaskResponseDTO> tasks, String currentQuery) {
        int currentPage = 1;
        int totalPages = Math.max(1, (int) Math.ceil((double) tasks.size() / PAGE_SIZE));

        while (true) {
            clearScreen();
            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;

            int fromIndex = (currentPage - 1) * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, tasks.size());
            List<TaskResponseDTO> pageTasks = tasks.subList(fromIndex, toIndex);

            printTableHeader();
            if (pageTasks.isEmpty()) {
                System.out.println("No tasks found.");
            } else {
                for (TaskResponseDTO task : pageTasks) {
                    printTaskRow(task);
                }
            }

            if (currentQuery != null) {
                System.out.printf("Page %d of %d (for the search query \"%s\")%n", currentPage, totalPages, currentQuery);
            } else {
                System.out.printf("Page %d of %d (all tasks)%n", currentPage, totalPages);
            }
            printPaginationControls(currentPage, totalPages);
            System.out.print("> ");

            String rawInput = scanner.nextLine().trim();

            // Глобальная кнопка возврата в главное меню
            if (rawInput.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            String action = parseAction(rawInput, currentPage, totalPages);

            switch (action) {
                case "SEARCH" -> { return "SEARCH"; }
                case "NEXT" -> { currentPage++; continue; }
                case "PREV" -> { currentPage--; continue; }

                case "EDIT" -> {
                    Long id = readValidTaskIdFromDTOList(tasks, "Enter Task ID to edit (or 'M' for Main Menu): ");
                    if (id != null) {
                        editSpecificTask(id);
                        return "REFRESH";
                    }
                }
                case String editCmd when editCmd.startsWith("EDIT:") -> {
                    String idStr = editCmd.substring(5);
                    try {
                        long parsedId = Long.parseLong(idStr);
                        if (tasks.stream().anyMatch(t -> t.id().equals(parsedId))) {
                            editSpecificTask(parsedId);
                            return "REFRESH";
                        } else {
                            System.out.println("Task with ID " + parsedId + " not found in the current list.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ID format.");
                    }
                }

                case "DELETE" -> {
                    Long id = readValidTaskIdFromDTOList(tasks, "Enter Task ID to delete (or 'M' for Main Menu): ");
                    if (id != null) {
                        deleteSpecificTask(id);
                        return "REFRESH";
                    }
                }
                case String delCmd when delCmd.startsWith("DELETE:") -> {
                    String idStr = delCmd.substring(7);
                    try {
                        long parsedId = Long.parseLong(idStr);
                        if (tasks.stream().anyMatch(t -> t.id().equals(parsedId))) {
                            deleteSpecificTask(parsedId);
                            return "REFRESH";
                        } else {
                            System.out.println("Task with ID " + parsedId + " not found in the current list.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ID format.");
                    }
                }
                default -> {
                    System.out.println("Invalid command.");
                }
            }
        }
    }

    private void createTask() {
        clearScreen();
        System.out.println("--- CREATE TASK ---");
        System.out.println("Type 'M' at any prompt to return to Main Menu");
        System.out.println("-------------------------------------------------");

        String title = readValidatedTitle();
        String description = readValidatedDescription();

        try {
            TaskRequestDTO requestDTO = TaskRequestDTO.forCreation(title, description);
            taskService.createTask(requestDTO);
            System.out.println("\nTask created successfully!");
        } catch (ValidationException e) {
            System.out.println("\nError: " + e.getMessage());
        }
    }

    private void editSpecificTask(Long id) {
        TaskResponseDTO taskDTO = taskService.getTaskById(id).orElse(null);
        if (taskDTO == null) {
            System.out.println("\nTask not found.");
            return;
        }

        while (true) {
            clearScreen();
            System.out.println("--- EDIT TASK #" + id + " ---");
            System.out.println("Type 'M' at any prompt to return to Main Menu");
            System.out.println("Current Title: " + taskDTO.title());
            System.out.println("Current Desc : " + taskDTO.description());
            System.out.println("Current Stat : " + taskDTO.status());
            System.out.println("----------------------------------------");

            String title = readValidatedTitleWithSkip("New Title: ", taskDTO.title());
            String description = readValidatedDescriptionWithSkip("New Description: ", taskDTO.description());

            System.out.println("Available statuses: PENDING, ACTIVE, DONE, CANCEL.");
            TaskStatus status = readValidatedStatusWithSkip("New Status ([P] | [A] | [D] | [C] or full name): ", taskDTO.status());

            try {
                TaskRequestDTO updateDTO = TaskRequestDTO.forUpdate(
                        title.isBlank() ? null : title,
                        description.isBlank() ? null : description,
                        status
                );

                taskService.updateTask(id, updateDTO);
                System.out.println("\nTask updated successfully!");
                break; // Выход из цикла редактирования, возврат к списку задач
            } catch (ValidationException e) {
                System.out.println("\nError: " + e.getMessage());
            }
        }
    }

    // === ХЕЛПЕРЫ ВАЛИДАЦИИ ===

    private String readValidatedTitle() {
        while (true) {
            String input = readInputWithEscape("Title: ");
            try {
                ValidationUtils.validateTitle(input);
                return input;
            } catch (ValidationException e) {
                System.out.println(e.getMessage() + " Try again.");
            }
        }
    }

    private String readValidatedTitleWithSkip(String prompt, String currentValue) {
        while (true) {
            String input = readInputWithEscape(prompt);
            if (input.isBlank()) return currentValue;
            try {
                ValidationUtils.validateTitle(input);
                return input;
            } catch (ValidationException e) {
                System.out.println(e.getMessage() + " Try again.");
            }
        }
    }

    private String readValidatedDescription() {
        while (true) {
            String input = readInputWithEscape("Description: ");
            try {
                ValidationUtils.validateDescription(input);
                return input;
            } catch (ValidationException e) {
                System.out.println(e.getMessage() + " Try again.");
            }
        }
    }

    private String readValidatedDescriptionWithSkip(String prompt, String currentValue) {
        while (true) {
            String input = readInputWithEscape(prompt);
            if (input.isBlank()) return currentValue;
            try {
                ValidationUtils.validateDescription(input);
                return input;
            } catch (ValidationException e) {
                System.out.println(e.getMessage() + " Try again.");
            }
        }
    }

    private TaskStatus readValidatedStatusWithSkip(String prompt, TaskStatus currentStatus) {
        while (true) {
            String input = readInputWithEscape(prompt);
            if (input.isBlank()) return currentStatus;

            TaskStatus newStatus = parseStatus(input);
            if (newStatus != null) {
                return newStatus;
            } else {
                System.out.println("Invalid status format. Try again (P, A, D, C).");
            }
        }
    }

    private int readValidMenuOption() {
        while (true) {
            System.out.print("Select option (1-4): ");
            String input = scanner.nextLine().trim();
            if (input.equals("1") || input.equals("2") || input.equals("3") || input.equals("4")) {
                return Integer.parseInt(input);
            }
            System.out.println("Invalid option. Please enter a number between 1 and 4.");
        }
    }

    private String readValidSearchQuery() {
        while (true) {
            System.out.print("Enter search query (or 'M' for Main Menu): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            if (!input.isBlank()) {
                return input;
            }
            System.out.println("Search query cannot be empty. Please enter at least 1 character.");
        }
    }

    private Long readValidTaskIdFromDTOList(List<TaskResponseDTO> validTasks, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            try {
                long id = Long.parseLong(input);
                boolean exists = validTasks.stream().anyMatch(t -> t.id().equals(id));

                if (exists) {
                    return id;
                } else {
                    System.out.println("Task with ID " + id + " not found in the current list. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid ID format. Please enter a valid number (or 'M' to cancel).");
            }
        }
    }

    private void deleteSpecificTask(Long id) {
        System.out.print("\nAre you sure you want to delete task #" + id + "? (y/n): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("y")) {
            taskService.deleteTask(id);
            System.out.println("Task deleted successfully!");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    private void clearDatabaseAndExit() {
        clearScreen();
        System.out.println("WARNING: DANGEROUS ACTION");
        System.out.println("This will PERMANENTLY DELETE the entire application folder:");
        System.out.println("   " + AppConfig.getAppDir());
        System.out.println("All tasks, logs, and settings will be lost FOREVER.");
        System.out.println();
        System.out.print("Type 'DELETE' to confirm, or 'M' to cancel: ");

        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("DELETE")) {
            System.out.println("\nDeleting application data...");
            boolean success = AppConfig.deleteAppDirectory();

            if (success) {
                System.out.println("Application data cleared successfully.");
                System.out.println("The application will now exit.");
            } else {
                System.out.println("Failed to delete some files. They might be in use.");
            }
            System.exit(0);
        } else {
            System.out.println("\nOperation cancelled.");
        }
    }

    // === ОТРИСОВКА ===

    private void printTableHeader() {
        int totalWidth = COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10;
        System.out.println("=".repeat(totalWidth));
        System.out.printf("%" + COL_ID + "s | %-" + COL_TITLE + "s | %-" + COL_DESC + "s | %s%n",
                "ID", "TITLE", "DESCRIPTION", "STATUS");
        System.out.println("=".repeat(totalWidth));
    }

    private void printTaskRow(TaskResponseDTO task) {
        List<String> titleLines = wrapText(task.title(), COL_TITLE);
        List<String> descLines = wrapText(task.description(), COL_DESC);

        int linesCount = Math.max(titleLines.size(), descLines.size());
        if (linesCount == 0) linesCount = 1;

        for (int i = 0; i < linesCount; i++) {
            String idStr = (i == 0) ? String.format("%" + COL_ID + "d", task.id()) : " ".repeat(COL_ID);
            String titleStr = (i < titleLines.size()) ? String.format("%-" + COL_TITLE + "s", titleLines.get(i)) : " ".repeat(COL_TITLE);
            String descStr = (i < descLines.size()) ? String.format("%-" + COL_DESC + "s", descLines.get(i)) : " ".repeat(COL_DESC);
            String statusStr = (i == 0) ? task.status().toString() : "";

            System.out.printf("%s | %s | %s | %s%n", idStr, titleStr, descStr, statusStr);
        }
        System.out.println("-".repeat(COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10));
    }

    private void printPaginationControls(int currentPage, int totalPages) {
        if (totalPages <= 1) {
            System.out.println("[M]enu | [S]earch");
        } else {
            List<String> controls = new ArrayList<>();
            if (currentPage > 1) controls.add("[P]rev");
            if (currentPage < totalPages) controls.add("[N]ext");
            controls.add("[M]enu");
            controls.add("[S]earch");
            System.out.println(String.join(" | ", controls));
        }
        System.out.println("OR type [E]<id> to Edit | [D]<id> to Delete (e.g., E10, D2)");
    }

    private List<String> wrapText(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        if (text.length() <= width) {
            lines.add(text);
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            while (word.length() > width) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }

            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= width) {
                currentLine.append(" ").append(word);
            } else {
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

    private void clearScreen() {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
    }

    private String readInputWithEscape(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        if (input.equalsIgnoreCase("M")) {
            throw new ReturnToMainMenuException();
        }
        return input;
    }

    private String parseAction(String input, int currentPage, int totalPages) {
        if (input == null || input.isBlank()) return "INVALID";

        if (input.matches("^[ED]\\d+$")) {
            char action = input.charAt(0);
            String id = input.substring(1);
            return (action == 'E' ? "EDIT:" : "DELETE:") + id;
        }

        return switch (input.toUpperCase()) {
            case "S" -> "SEARCH";
            case "E" -> "EDIT";
            case "D" -> "DELETE";
            case "N" -> (currentPage < totalPages) ? "NEXT" : "INVALID";
            case "P" -> (currentPage > 1) ? "PREV" : "INVALID";
            default -> "INVALID";
        };
    }

    private TaskStatus parseStatus(String input) {
        if (input == null || input.isBlank()) return null;
        return switch (input) {
            case "P", "PENDING", "pending" -> TaskStatus.PENDING;
            case "A", "ACTIVE", "active" -> TaskStatus.ACTIVE;
            case "D", "DONE", "done" -> TaskStatus.DONE;
            case "C", "CANCEL", "cancel" -> TaskStatus.CANCEL;
            default -> null;
        };
    }
}