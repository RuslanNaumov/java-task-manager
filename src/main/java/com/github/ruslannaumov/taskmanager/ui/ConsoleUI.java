package com.github.ruslannaumov.taskmanager.ui;

import com.github.ruslannaumov.taskmanager.config.AppConfig;
import com.github.ruslannaumov.taskmanager.dto.TaskRequestDTO;
import com.github.ruslannaumov.taskmanager.dto.TaskResponseDTO;
import com.github.ruslannaumov.taskmanager.exception.DatabaseException;
import com.github.ruslannaumov.taskmanager.exception.ReturnToMainMenuException;
import com.github.ruslannaumov.taskmanager.exception.ValidationException;
import com.github.ruslannaumov.taskmanager.model.TaskStatus;
import com.github.ruslannaumov.taskmanager.service.ITaskService;
import com.github.ruslannaumov.taskmanager.util.ValidationUtils;

import static com.github.ruslannaumov.taskmanager.util.ColorUtils.*;

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
                        System.out.println(green("Goodbye!"));
                        pause();
                    }
                }
            } catch (ReturnToMainMenuException e) {
                clearScreen();
            } catch (DatabaseException e) {
                // Сюда попадет любая ошибка БД из createTask, viewTasks, delete и т.д.
                displayFatalDatabaseError(e.getMessage());
                running = false; // На случай, если System.exit(1) не сработает
            }

            if (running) {
                clearScreen();
            }
        }
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
                    System.out.printf(red("No tasks found for query: \"%s\"%n"), currentQuery);
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
        // 1. Если задач нет вообще, показываем только Empty State
        if (tasks.isEmpty()) {
            while (true) {
                clearScreen();
                System.out.println(red("No tasks found."));
                System.out.println(yellow("Return to [M]enu and create  task"));

                String rawInput = JLineInputHelper.readLine(blue("> ")).trim();
                if (rawInput.equalsIgnoreCase("M")) {
                    throw new ReturnToMainMenuException();
                }
                // Игнорируем любой другой ввод, ждем только 'M'
            }
        }

        // 2. ОСНОВНАЯ ЛОГИКА: Выполняется ТОЛЬКО если задачи есть
        int currentPage = 1;
        int totalPages=taskService.getTotalPages(tasks.size(), PAGE_SIZE);
        while (true) {
            clearScreen();
            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;

            int fromIndex = (currentPage - 1) * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, tasks.size());
            List<TaskResponseDTO> pageTasks = tasks.subList(fromIndex, toIndex);

            printTableHeader();
            for (TaskResponseDTO task : pageTasks) {
                printTaskRow(task);
            }

            if (currentQuery != null) {
                System.out.printf("Page %d of %d (for the search query \"%s\" - %d tasks)%n",
                        currentPage, totalPages, currentQuery, tasks.size());
            } else {
                System.out.printf("Page %d of %d (all tasks - %d)%n",
                        currentPage, totalPages, tasks.size());
            }

            System.out.println();
            printPaginationControls(currentPage, totalPages);
            System.out.println();

            String rawInput = JLineInputHelper.readLine(blue("> ")).trim();

            if (rawInput.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            String action = parseAction(rawInput, currentPage, totalPages);

            switch (action) {
                case "SEARCH" -> { return "SEARCH"; }
                case "NEXT" -> { currentPage++; continue; }
                case "PREV" -> { currentPage--; continue; }
                case "MENU" -> { throw new ReturnToMainMenuException(); }

                case String viewCmd when viewCmd.startsWith("VIEW:") -> {
                    String idStr = viewCmd.substring(5);
                    try {
                        long parsedId = Long.parseLong(idStr);
                        viewSpecificTask(parsedId);
                        return "REFRESH";
                    } catch (NumberFormatException e) {
                        System.out.println(red("Invalid ID format: " + idStr));
                        pause();
                    }
                }

                case String editCmd when editCmd.startsWith("EDIT:") -> {
                    String idStr = editCmd.substring(5);
                    try {
                        long parsedId = Long.parseLong(idStr);
                        editSpecificTask(parsedId);
                        return "REFRESH";
                    } catch (NumberFormatException e) {
                        System.out.println(red("Invalid ID format: " + idStr));
                        pause();
                    }
                }

                case String delCmd when delCmd.startsWith("DELETE:") -> {
                    String idStr = delCmd.substring(7);
                    try {
                        long parsedId = Long.parseLong(idStr);
                        deleteSpecificTask(parsedId);
                        return "REFRESH";
                    } catch (NumberFormatException e) {
                        System.out.println(red("Invalid ID format: " + idStr));
                        pause();
                    }
                }

                default -> {
                    System.out.println(red("Invalid command"));
                    pause();
                }
            }
        }
    }

    private void createTask() {
        // ПРОВЕРКА ДОСТУПНОСТИ БД
        try {
            taskService.getTaskCount();
        } catch (DatabaseException e) {
            displayFatalDatabaseError(e.getMessage());
            return;
        }

        try {
            Runnable emptyContext = () -> {
                System.out.println(bold("CREATE TASK"));
                System.out.println();
                System.out.println(bold("You must fill in the fields: title, description, status"));
                System.out.println();
                System.out.println(yellow("Type [M] at any prompt to return to Main Menu"));
                System.out.println();
            };

            String title = readValidatedField("Title", "", ValidationUtils::validateTitle, false, emptyContext);

            Runnable descContext = () -> {
                emptyContext.run();
                System.out.println(bold("Title: " + title));
            };
            String description = readValidatedField("Description", "", ValidationUtils::validateDescription, false, descContext);

            Runnable statusContext = () -> {
                emptyContext.run();
                System.out.println(bold("Title: " + title));
                System.out.println(bold("Description: " + description));
            };
            TaskStatus status = readValidatedStatus(TaskStatus.PENDING, statusContext);

            TaskRequestDTO requestDTO = new TaskRequestDTO(title, description, status);
            taskService.createTask(requestDTO);

            System.out.println(green("Task created successfully!"));
            pause();

        } catch (DatabaseException e) {
            // ПЕРЕХВАТ ОШИБКИ БД ПРИ СОХРАНЕНИИ
            displayFatalDatabaseError(e.getMessage());
        } catch (ReturnToMainMenuException e) {
            System.out.println(red("Task creation cancelled."));
            pause();
            throw e;
        } catch (Exception e) {
            System.out.println(red("Unexpected error: " + e.getMessage()));
            pause();
        }
    }

    private void editSpecificTask(Long id) {
        // ПРОВЕРКА ДОСТУПНОСТИ БД
        try {
            taskService.getTaskCount();
        } catch (DatabaseException e) {
            displayFatalDatabaseError(e.getMessage());
            return;
        }

        TaskResponseDTO taskDTO = taskService.getTaskById(id).orElse(null);
        if (taskDTO == null) {
            System.out.println(red("Task with ID " + id + " not found."));
            pause();
            return;
        }

        Runnable baseEditContext = () -> {
            System.out.println(bold("EDIT TASK #" + id));
            System.out.println();
            printTaskDetails(taskDTO);
            System.out.println();
            System.out.println(yellow("Press Enter to keep current value. Type [M] to cancel."));
            System.out.println();
        };

        try {
            String finalTitle = readValidatedField("Title", taskDTO.title(), ValidationUtils::validateTitle, false, baseEditContext);

            Runnable descContext = () -> {
                baseEditContext.run();
                System.out.println(bold("Title: " + finalTitle));
            };
            String finalDesc = readValidatedField("Description", taskDTO.description() != null ? taskDTO.description() : "", ValidationUtils::validateDescription, true, descContext);

            Runnable statusContext = () -> {
                baseEditContext.run();
                System.out.println(bold("Title: " + finalTitle));
                System.out.println(bold("Description: " + (finalDesc.isEmpty() ? "(empty)" : finalDesc)));
            };
            TaskStatus finalStatus = readValidatedStatus(taskDTO.status(), statusContext);

            TaskRequestDTO updateDTO = new TaskRequestDTO(finalTitle, finalDesc, finalStatus);
            taskService.updateTask(id, updateDTO);
            System.out.println(green("Task updated successfully!"));
            pause();

        } catch (DatabaseException e) {
            // ПЕРЕХВАТ ОШИБКИ БД ПРИ СОХРАНЕНИИ
            displayFatalDatabaseError(e.getMessage());
        } catch (ReturnToMainMenuException e) {
            System.out.println(red("Edit cancelled."));
            pause();
            throw e;
        } catch (Exception e) {
            System.out.println(red("Unexpected error: " + e.getMessage()));
            pause();
        }
    }

    private void deleteSpecificTask(Long id) {
        // 1. Проверка существования задачи (БЕЗ очистки экрана)
        var taskDTO = taskService.getTaskById(id).orElse(null);
        if (taskDTO == null) {
            System.out.println(red("Task with ID " + id + " not found."));
            pause();
            return; // Возвращаемся к списку, экран не стерт
        }

        // 2. Если задача найдена, очищаем экран и показываем подтверждение
        clearScreen();
        System.out.println(bold("DELETE TASK #" + id));
        System.out.println();
        printTaskDetails(taskDTO);
        System.out.println();
        System.out.println(yellow("Warning: This action cannot be undone."));
        System.out.println();

        // 3. Подтверждение удаления
        String confirm = JLineInputHelper.readLine(blue("Are you sure you want to delete task #" + id + " (y/n): ")).trim();

        if (confirm.equalsIgnoreCase("y")) {
            taskService.deleteTask(id);
            System.out.println(green("Task deleted successfully!"));
            pause();
        } else {
            System.out.println(red("Deletion cancelled."));
            pause();
        }
    }

    private void viewSpecificTask(Long id) {
        // 1. Проверка существования задачи (БЕЗ очистки экрана)
        var taskDTO = taskService.getTaskById(id).orElse(null);
        if (taskDTO == null) {
            System.out.println(red("Task with ID " + id + " not found."));
            pause();
            return;
        }

        // Флаг для управления показом ошибки и последующей перерисовкой
        boolean showError = false;

        while (true) {
            // Если в предыдущем цикле была ошибка, показываем её, делаем паузу и сбрасываем флаг
            if (showError) {
                System.out.println(red("Invalid command. Please type [B] or [M]."));
                pause();
                showError = false;
            }

            // 2. Очищаем экран и рисуем детали задачи (выполняется при первом входе и после каждой ошибки)
            clearScreen();
            System.out.println(bold("VIEW TASK #" + id));
            System.out.println();
            System.out.printf(bold("ID: %d%n"), taskDTO.id());
            System.out.printf(bold("Title: %s%n"), taskDTO.title());
            System.out.printf(bold("Description: %s%n"), taskDTO.description() != null ? taskDTO.description() : "(empty)");
            System.out.printf(bold("Status: %s%n"), taskDTO.status());
            System.out.printf(bold("Created: %s%n"), taskDTO.createdAt() != null ? taskDTO.createdAt().toString() : "N/A");
            System.out.printf(bold("Updated: %s%n"), taskDTO.updatedAt() != null ? taskDTO.updatedAt().toString() : "N/A");
            System.out.println();
            System.out.println(yellow("Type [B] to go back to the list, or [M] to return to the Main Menu."));
            System.out.println();

            // 3. Читаем ввод
            String input = JLineInputHelper.readLine(blue("> ")).trim().toUpperCase();

            if (input.equals("B")) {
                return; // Выходим, возвращаем "REFRESH" в список
            } else if (input.equals("M")) {
                throw new ReturnToMainMenuException(); // Возврат в главное меню
            } else {
                // Если ввод некорректный, устанавливаем флаг.
                // На следующей итерации цикла сработает блок if (showError), покажет ошибку, сделает pause() и перерисует экран.
                showError = true;
            }
        }
    }

    // === ХЕЛПЕРЫ ВАЛИДАЦИИ ===

    private String readValidatedField(String fieldName, String currentValue,
                                      java.util.function.Consumer<String> validator,
                                      boolean allowEmpty,
                                      Runnable drawContext) {
        String currentInput = currentValue != null ? currentValue : "";

        while (true) {
            clearScreen();
            if (drawContext != null) {
                drawContext.run(); // Рисуем контекст (карточку задачи, предыдущие поля)
            }

            String input = JLineInputHelper.readLineWithDefault(blue(fieldName + ": "), currentInput).trim();

            if (input.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            if (input.isBlank()) {
                if (allowEmpty) {
                    return currentValue != null ? currentValue : "";
                } else {
                    System.out.println(red(fieldName + " cannot be empty."));
                    currentInput = "";
                    pause();
                    continue;
                }
            }

            try {
                validator.accept(input);
                return input;
            } catch (ValidationException e) {
                System.out.println(red(e.getMessage()));
                currentInput = input;
                pause();
            }
        }
    }

    private TaskStatus readValidatedStatus(TaskStatus currentStatus, Runnable drawContext) {
        String currentInput = currentStatus != null ? currentStatus.name() : "PENDING";

        while (true) {
            clearScreen();
            if (drawContext != null) {
                drawContext.run();
            }
            System.out.println(yellow("Available statuses: PENDING [P], ACTIVE [A], DONE [D], CANCEL [C]"));

            String input = JLineInputHelper.readLineWithDefault(blue("Status: "), currentInput).trim();

            if (input.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            if (input.isBlank()) {
                return currentStatus != null ? currentStatus : TaskStatus.PENDING;
            }

            TaskStatus parsedStatus = parseStatus(input);
            if (parsedStatus != null) {
                return parsedStatus;
            } else {
                System.out.println(red("Invalid status format. Try again."));
                currentInput = input;
                pause();
            }
        }
    }

    private int readValidMenuOption() {
        while (true) {
            clearScreen();
            printMainMenu();
            String input = JLineInputHelper.readLine(blue("Select option (1-4): ")).trim();
            if (input.equals("1") || input.equals("2") || input.equals("3") || input.equals("4")) {
                return Integer.parseInt(input);
            }
            System.out.println(red("Invalid option. Please enter a number between 1 and 4."));
            pause();
        }
    }

    private String readValidSearchQuery() {
        while (true) {
            String input = JLineInputHelper.readLine(blue("Enter search query (or 'M' for Main Menu): ")).trim();
            if (input.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }
            if (!input.isBlank()) {
                return input;
            }
            System.out.println(red("Search query cannot be empty. Please enter at least 1 character."));
        }
    }

    private Long readValidTaskIdFromDTOList(List<TaskResponseDTO> validTasks, String prompt) {
        while (true) {
            String input = JLineInputHelper.readLine(blue(prompt)).trim();

            if (input.equalsIgnoreCase("M")) {
                throw new ReturnToMainMenuException();
            }

            try {
                long id = Long.parseLong(input);
                boolean exists = validTasks.stream().anyMatch(t -> t.id().equals(id));

                if (exists) {
                    return id;
                } else {
                    System.out.println(red("Task with ID " + id + " not found in the current list. Try again."));
                    pause();
                }
            } catch (NumberFormatException e) {
                System.out.println(red("Invalid ID format. Please enter a valid number (or 'M' to cancel)."));
                pause();
            }
        }
    }

    private void clearDatabaseAndExit() {
        clearScreen();
        System.out.println(red("WARNING: DANGEROUS ACTION"));
        System.out.println();
        System.out.println("This will PERMANENTLY DELETE the entire application folder:");
        System.out.println(AppConfig.getAppDir());
        System.out.println("All tasks, logs, and settings will be lost FOREVER.");
        System.out.println();

        String confirm = JLineInputHelper.readLine(blue("Type \"DELETE\" or any character to cancel: ")).trim();

        if (confirm.equalsIgnoreCase("DELETE")) {
            System.out.println(green("Deleting application data..."));
            boolean success = AppConfig.deleteAppDirectory();

            if (success) {
                System.out.println("Application data cleared successfully.");
                System.out.println("The application will now exit.");
            } else {
                System.out.println(red("Failed to delete some files. They might be in use."));
            }
            pause();
            System.exit(0);
        } else {
            System.out.println(red("Operation cancelled."));
            pause();
        }
    }

    // === ОТРИСОВКА ===

    private void printMainMenu() {
        System.out.println(bold("======= TASK MANAGER ======="));
        System.out.println();
        System.out.println();
        System.out.println(bold("1. ➕ Add Task"));
        System.out.println();
        System.out.println(bold("2. 📋 View Tasks"));
        System.out.println();
        System.out.println(bold("3. 🧹 Clear Database & Exit"));
        System.out.println();
        System.out.println(bold("4. 🚪 Exit"));
        System.out.println();
        System.out.println();
        System.out.println(bold("============================"));
        System.out.println();
    }

    private void printTableHeader() {
        int totalWidth = COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10;
        System.out.println(bold("=").repeat(totalWidth));
        System.out.printf(bold("%" + COL_ID + "s | %-" + COL_TITLE + "s | %-" + COL_DESC + "s | %s%n"),
                "ID", "TITLE", "DESCRIPTION", "STATUS");
        System.out.println(bold("=").repeat(totalWidth));
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

            System.out.printf(bold("%s | %s | %s | %s%n"), idStr, titleStr, descStr, statusStr);
        }
        System.out.println(bold("-").repeat(COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10));
    }

    private void printPaginationControls(int currentPage, int totalPages) {
        if (totalPages <= 1) {
            System.out.println(yellow("[M]enu | [S]earch"));
        } else {
            List<String> controls = new ArrayList<>();
            if (currentPage > 1) controls.add("[P]rev");
            if (currentPage < totalPages) controls.add("[N]ext");
            controls.add("[M]enu");
            controls.add("[S]earch");
            System.out.println(yellow(String.join(" | ", controls)));
        }
        System.out.println(yellow("OR Type [V]<id> to View | [E]<id> to Edit | [D]<id> to Delete (e.g., E10, D2)"));
    }

    private void printTaskDetails(TaskResponseDTO task) {
        System.out.printf(bold("ID: %d%n"), task.id());
        System.out.printf(bold("Title: %s%n"), task.title());
        System.out.printf(bold("Description: %s%n"), task.description() != null ? task.description() : "(empty)");
        System.out.printf(bold("Status: %s%n"), task.status());
        System.out.printf(bold("Created: %s%n"), task.createdAt() != null ? task.createdAt().toString() : "N/A");
        System.out.printf(bold("Updated: %s%n"), task.updatedAt() != null ? task.updatedAt().toString() : "N/A");
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
        String input = JLineInputHelper.readLine(blue(prompt)).trim();
        if (input.equalsIgnoreCase("M")) {
            throw new ReturnToMainMenuException();
        }
        return input;
    }

    public void displayFatalDatabaseError(String details) {
        clearScreen();

        // Визуальное выделение ошибки
        System.out.println(red("╔════════════════════════════════════════════════════════╗"));
        System.out.println(red("║                CRITICAL DATABASE ERROR                 ║"));
        System.out.println(red("╠════════════════════════════════════════════════════════╣"));
        System.out.println(red("║ Database connection lost or unavailable.               ║"));
        System.out.println(red("║ The application cannot function without the database.  ║"));
        System.out.println(red("╚════════════════════════════════════════════════════════╝"));
        System.out.println();
        System.out.println("Technical details: " + details);
        System.out.println();
        System.out.println("Please ensure the database file is not corrupted, locked,");
        System.out.println("or deleted, and that you have write permissions.");
        System.out.println();

        // Бесконечный цикл, пока пользователь не введет 'exit'
        while (true) {
            System.out.print(yellow("Type 'exit' to close the application: "));
            String input = JLineInputHelper.readLine(blue("> ")).trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Shutting down...");
                System.exit(1); // 1 означает, что программа завершилась с ошибкой
            }
        }
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private String parseAction(String input, int currentPage, int totalPages) {
        if (input == null || input.isBlank()) return "INVALID";

        String upper = input.toUpperCase().trim();

        if (upper.matches("^[EDV]\\d+$")) {
            char action = upper.charAt(0);
            String id = upper.substring(1);
            if (action == 'E') return "EDIT:" + id;
            if (action == 'D') return "DELETE:" + id;
            if (action == 'V') return "VIEW:" + id;
        }

        return switch (upper) {
            case "S" -> "SEARCH";
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

    private void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}