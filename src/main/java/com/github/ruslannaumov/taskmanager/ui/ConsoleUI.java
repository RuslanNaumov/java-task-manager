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

public class ConsoleUI {

    private final ITaskService taskService;
    private static final int PAGE_SIZE = 5;

    private static final int COL_ID = 5;
    private static final int COL_TITLE = 30;
    private static final int COL_DESC = 40;
    private static final int COL_STATUS = 15;

    public ConsoleUI(ITaskService taskService) {
        this.taskService = taskService;
    }

    public void start() {
        JLineInputHelper.init();
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
                    currentQuery = newQuery;
                    continue;
                }
            }

            String action = displayTaskListAndHandleActions(tasks, currentQuery);

            if ("SEARCH".equals(action)) {
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

            String rawInput = JLineInputHelper.readLine("> ").trim();

            if (rawInput.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            String action = parseAction(rawInput, currentPage, totalPages);

            switch (action) {
                case "SEARCH" -> { return "SEARCH"; }
                case "NEXT" -> { currentPage++; continue; }
                case "PREV" -> { currentPage--; continue; }
                case "MENU" -> { throw new ReturnToMainMenuException(); }

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
                        editSpecificTask(parsedId);
                        return "REFRESH";
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ID format: " + idStr);
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
                        deleteSpecificTask(parsedId);
                        return "REFRESH";
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid ID format: " + idStr);
                    }
                }

                default -> {
                    System.out.println("Invalid command: " + action);
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
            System.out.println("=== EDIT TASK #" + id + " ===");
            System.out.println("Tip: Edit pre-filled values with arrow keys. Press Enter to keep current value.");
            System.out.println("Type 'M' at any prompt to cancel and return to Main Menu.");
            System.out.println("-----------------------------------------------------------");

            String newTitle = JLineInputHelper.readLineWithDefault(
                    "Title: ",
                    taskDTO.title()
            );
            if (newTitle.equalsIgnoreCase("M")) {
                System.out.println("Edit cancelled.");
                return;
            }

            String currentDesc = taskDTO.description() != null ? taskDTO.description() : "";
            String newDesc = JLineInputHelper.readLineWithDefault(
                    "Description: ",
                    currentDesc
            );
            if (newDesc.equalsIgnoreCase("M")) {
                System.out.println("Edit cancelled.");
                return;
            }

            System.out.println("Available statuses: PENDING (P), ACTIVE (A), DONE (D), CANCEL (C)");
            String newStatusStr = JLineInputHelper.readLineWithDefault(
                    "Status: ",
                    taskDTO.status().name()
            );
            if (newStatusStr.equalsIgnoreCase("M")) {
                System.out.println("Edit cancelled.");
                return;
            }

            // Если пользователь нажал Enter (строка пустая), используем старое значение
            if (newTitle.isBlank()) newTitle = taskDTO.title();
            if (newDesc.isBlank()) newDesc = currentDesc;

            TaskStatus newStatus = taskDTO.status();
            if (!newStatusStr.isBlank()) {
                TaskStatus parsedStatus = parseStatus(newStatusStr);
                if (parsedStatus != null) {
                    newStatus = parsedStatus;
                } else {
                    System.out.println("Invalid status format. Keeping old status.");
                }
            }

            try {
                TaskRequestDTO updateDTO = new TaskRequestDTO(newTitle, newDesc, newStatus);
                taskService.updateTask(id, updateDTO);
                System.out.println("\nTask updated successfully!");
                break;
            } catch (ValidationException e) {
                System.out.println("\nError: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\nUnexpected error: " + e.getMessage());
                break;
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

    private int readValidMenuOption() {
        while (true) {
            String input = JLineInputHelper.readLine("Select option (1-4): ").trim();
            if (input.equals("1") || input.equals("2") || input.equals("3") || input.equals("4")) {
                return Integer.parseInt(input);
            }
            System.out.println("Invalid option. Please enter a number between 1 and 4.");
        }
    }

    private String readValidSearchQuery() {
        while (true) {
            String input = JLineInputHelper.readLine("Enter search query (or 'M' for Main Menu): ").trim();
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
            String input = JLineInputHelper.readLine(prompt).trim();

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
        String confirm = JLineInputHelper.readLine("\nAre you sure you want to delete task #" + id + "? (y/n): ").trim();
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

        String confirm = JLineInputHelper.readLine("Type 'DELETE' to confirm, or 'M' to cancel: ").trim();

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
        String input = JLineInputHelper.readLine(prompt).trim();
        if (input.equalsIgnoreCase("M")) {
            throw new ReturnToMainMenuException();
        }
        return input;
    }

    private String parseAction(String input, int currentPage, int totalPages) {
        if (input == null || input.isBlank()) return "INVALID";

        String upper = input.toUpperCase().trim();

        if (upper.matches("^[ED]\\d+$")) {
            char action = upper.charAt(0);
            String id = upper.substring(1);
            return (action == 'E' ? "EDIT:" : "DELETE:") + id;
        }

        return switch (upper) {
            case "S" -> "SEARCH";
            case "E" -> "EDIT";
            case "D" -> "DELETE";
            case "N" -> (currentPage < totalPages) ? "NEXT" : "INVALID";
            case "P" -> (currentPage > 1) ? "PREV" : "INVALID";
            case "M" -> "MENU";
            default -> "INVALID";
        };
    }

    private TaskStatus parseStatus(String input) {
        if (input == null || input.isBlank()) return null;
        return switch (input.toUpperCase()) {
            case "P", "PENDING" -> TaskStatus.PENDING;
            case "A", "ACTIVE" -> TaskStatus.ACTIVE;
            case "D", "DONE" -> TaskStatus.DONE;
            case "C", "CANCEL" -> TaskStatus.CANCEL;
            default -> null;
        };
    }
}