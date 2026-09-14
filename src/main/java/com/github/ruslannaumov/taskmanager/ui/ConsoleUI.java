package com.github.ruslannaumov.taskmanager.ui;

import com.github.ruslannaumov.taskmanager.config.AppConfig;
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
            printMainMenu();
            int choice = readInt("Select option: ");

            switch (choice) {
                case 1 -> createTask();
                case 2 -> viewTasks();
                case 3 -> clearDatabaseAndExit(); // <-- НОВЫЙ ПУНКТ
                case 4 -> {                       // <-- EXIT сдвинут на 4
                    running = false;
                    System.out.println("Goodbye!");
                }
                default -> {
                    System.out.println("Invalid option.");
                }
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

    // === ГЛАВНЫЙ ЦИКЛ УПРАВЛЕНИЯ ЗАДАЧАМИ ===
    private void viewTasks() {
        String currentQuery = null; // null означает "показать все задачи"

        while (true) {
            // 1. Определяем, какие данные загружать
            List<Task> tasks;

            if (currentQuery == null) {
                tasks = taskService.getAllTasks();
            } else {
                tasks = taskService.searchTasks(currentQuery);

                // ЕСЛИ ПОИСК НЕ ДАЛ РЕЗУЛЬТАТОВ — НЕ РИСУЕМ ТАБЛИЦУ
                if (tasks.isEmpty()) {
                    clearScreen();
                    System.out.printf("No tasks found for query: \"%s\"%n", currentQuery);
                    System.out.println("\nEnter new search query, [M] for Main Menu, or press Enter to show all tasks:");
                    System.out.print("> ");
                    String input = scanner.nextLine().trim();

                    if (input.equalsIgnoreCase("M")) {
                        clearScreen();
                        return; // Выход в главное меню
                    } else if (input.isBlank()) {
                        currentQuery = null; // Сброс к "Все задачи"
                    } else {
                        currentQuery = input; // Новый запрос
                    }
                    continue; // Перезапускаем цикл с новым currentQuery
                }
            }

            // 2. Отображаем таблицу и обрабатываем действия
            String action = displayTaskListAndHandleActions(tasks, currentQuery != null);

            // 3. Реагируем на действие
            if (action.equals("EXIT")) {
                clearScreen();
                return; // Выход в главное меню
            }
            else if (action.equals("SEARCH")) {
                clearScreen();
                System.out.print("Enter search query (or press Enter to show all tasks): ");
                String newQuery = scanner.nextLine().trim();

                if (newQuery.isBlank()) {
                    currentQuery = null; // Сброс к "Все задачи"
                } else {
                    currentQuery = newQuery; // Установка нового фильтра
                }
                // Цикл продолжается, данные перезагрузятся с новым currentQuery
            }
            // Если action.equals("REFRESH"), цикл просто продолжается,
            // заново загружая актуальные данные для текущего currentQuery (все или поиск)
        }
    }

    // === УНИВЕРСАЛЬНЫЙ ЭКРАН ТАБЛИЦЫ ===
    // Возвращает: "EXIT" (выход), "SEARCH" (перейти к поиску), "REFRESH" (обновить таблицу)
    private String displayTaskListAndHandleActions(List<Task> tasks, boolean isSearchMode) {
        int currentPage = 1;
        int totalPages = Math.max(1, (int) Math.ceil((double) tasks.size() / PAGE_SIZE));

        while (true) {
            clearScreen();

            // Защита от выхода за границы страниц
            if (currentPage > totalPages) currentPage = totalPages;
            if (currentPage < 1) currentPage = 1;

            int fromIndex = (currentPage - 1) * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, tasks.size());
            List<Task> pageTasks = tasks.subList(fromIndex, toIndex);

            printTableHeader();

            if (pageTasks.isEmpty()) {
                System.out.println("No tasks found.");
            } else {
                for (Task task : pageTasks) {
                    printTaskRow(task);
                }
            }

            System.out.printf("Page %d of %d%n", currentPage, totalPages);

            System.out.println("[N]ext | [P]rev | [M]enu | [S]earch");
            System.out.println("OR type [E] to Edit | [D] to Delete");
            System.out.print("> ");

            // ЧИТАЕМ ВВОД БЕЗ .toUpperCase(), чтобы сохранить оригинальный регистр для строгой проверки
            String rawInput = scanner.nextLine().trim();

            // Передаем ввод в наш строгий парсер
            String action = parseAction(rawInput);

            // Обрабатываем результат парсинга
            switch (action) {
                case "EXIT" -> {
                    return "EXIT";
                }
                case "SEARCH" -> {
                    return "SEARCH";
                }
                case "NEXT" -> {
                    if (currentPage < totalPages) currentPage++;
                    continue;
                }
                case "PREV" -> {
                    if (currentPage > 1) currentPage--;
                    continue;
                }
                case "EDIT" -> {
                    Long id = promptForTaskId("Enter Task ID to edit: ", tasks);
                    if (id != null) {
                        editSpecificTask(id);
                        return "REFRESH"; // Сигнал перезагрузить текущий вид
                    }
                }
                case "DELETE" -> {
                    Long id = promptForTaskId("Enter Task ID to delete: ", tasks);
                    if (id != null) {
                        deleteSpecificTask(id);
                        return "REFRESH";
                    }
                }
                default -> {
                    // Сюда попадет всё, что не соответствует строгим правилам (например, "p", "Edit", "m")
                    System.out.println("Invalid command. Use uppercase single letter (P, N, M, S, E, D).");
                    // Небольшая пауза, чтобы пользователь успел прочитать сообщение об ошибке перед перерисовкой
                    try { Thread.sleep(1500); } catch (InterruptedException e) {}
                }
            }
        }
    }

    // === ДЕЙСТВИЯ ===

    private void createTask() {
        clearScreen();
        System.out.println("--- CREATE TASK ---");
        System.out.println("Type 'M' at any prompt to return to Main Menu");
        System.out.println("-------------------------------------------------");

        String title = readInputWithEscape("Title: ");
        if (title == null) return; // Пользователь ввел 'M', выходим

        String description = readInputWithEscape("Description: ");
        if (description == null) return; // Пользователь ввел 'M', выходим

        taskService.createTask(title, description);
        System.out.println("\nTask created successfully!");
    }

    private void editSpecificTask(Long id) {
        Optional<Task> optionalTask = taskService.getTaskById(id);
        if (optionalTask.isEmpty()) return;

        Task task = optionalTask.get();

        clearScreen();
        System.out.println("--- EDIT TASK #" + id + " ---");
        System.out.println("Type 'M' at any prompt to cancel and return to Menu");
        System.out.println("Current Title: " + task.getTitle());
        System.out.println("Current Desc : " + task.getDescription());
        System.out.println("Current Stat : " + task.getStatus());
        System.out.println("----------------------------------------");

        String title = readInputWithEscape("New Title (or Enter to skip): ");
        if (title == null) return;
        if (title.isBlank()) title = task.getTitle();

        String description = readInputWithEscape("New Description (or Enter to skip): ");
        if (description == null) return;
        if (description.isBlank()) description = task.getDescription();

        System.out.println("Available statuses: PENDING, ACTIVE, DONE, CANCEL.");
        String statusInput = readInputWithEscape("New Status ([P] | [A] | [D] | [C] or full name, or Enter to skip): ");
        if (statusInput == null) return;

        TaskStatus status = task.getStatus();
        if (!statusInput.isBlank()) {
            TaskStatus newStatus = parseStatus(statusInput);
            if (newStatus != null) {
                status = newStatus;
            } else {
                System.out.println("Invalid status format. Keeping old status: " + task.getStatus());
            }
        }

        taskService.updateTask(id, title, description, status);
        System.out.println("\nTask updated successfully!");
    }

    private Long promptForTaskId(String prompt, List<Task> validTasks) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        try {
            long id = Long.parseLong(input);
            boolean exists = validTasks.stream().anyMatch(t -> t.getId().equals(id));
            if (exists) {
                return id;
            } else {
                System.out.println("Task with ID " + id + " not found in this list.");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid ID format. Please enter a number.");
            return null;
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

    // === ОЧИСТКА И ВЫХОД ===
    private void clearDatabaseAndExit() {
        clearScreen();
        System.out.println("WARNING: DANGEROUS ACTION");
        System.out.println("This will PERMANENTLY DELETE the entire application folder:");
        System.out.println("   " + AppConfig.getAppDir());
        System.out.println("All tasks, logs, and settings will be lost FOREVER.");
        System.out.println();
        System.out.print("Type 'DELETE' to confirm, or anything else to cancel: ");

        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("DELETE")) {
            System.out.println("\nDeleting application data...");
            boolean success = AppConfig.deleteAppDirectory();

            if (success) {
                System.out.println("Application data cleared successfully.");
                System.out.println("The application will now exit.");
            } else {
                System.out.println("Failed to delete some files. They might be in use by another program.");
            }

            // Даем пользователю 1 секунду прочитать сообщение перед закрытием
            try { Thread.sleep(1000); } catch (InterruptedException e) {}
            // Принудительное завершение работы JVM
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

    private void printTaskRow(Task task) {
        List<String> titleLines = wrapText(task.getTitle(), COL_TITLE);
        List<String> descLines = wrapText(task.getDescription(), COL_DESC);

        int linesCount = Math.max(titleLines.size(), descLines.size());
        if (linesCount == 0) linesCount = 1;

        for (int i = 0; i < linesCount; i++) {
            String idStr = (i == 0) ? String.format("%" + COL_ID + "d", task.getId()) : " ".repeat(COL_ID);
            String titleStr = (i < titleLines.size()) ? String.format("%-" + COL_TITLE + "s", titleLines.get(i)) : " ".repeat(COL_TITLE);
            String descStr = (i < descLines.size()) ? String.format("%-" + COL_DESC + "s", descLines.get(i)) : " ".repeat(COL_DESC);
            String statusStr = (i == 0) ? task.getStatus().toString() : "";

            System.out.printf("%s | %s | %s | %s%n", idStr, titleStr, descStr, statusStr);
        }
        System.out.println("-".repeat(COL_ID + COL_TITLE + COL_DESC + COL_STATUS + 10));
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
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // === УМНЫЕ ХЕЛПЕРЫ ===

    /**
     * Читает ввод пользователя. Если введено "M", возвращает null (сигнал к выходу в меню).
     */
    private String readInputWithEscape(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();

        // Строгий выход: только заглавная M
        if (input.equals("M")) {
            return null;
        }
        return input;
    }

    /**
     * Строго парсит команду действия.
     * Разрешает ТОЛЬКО:
     * 1. Одну заглавную букву (P, N, M, S, E, D)
     * Возвращает "INVALID", если ввод не соответствует правилам.
     */
    private String parseAction(String input) {
        if (input == null || input.isBlank()) {
            return "INVALID";
        }

        return switch (input) {
            // Выход / Назад
            case "M" -> "EXIT";
            // Поиск
            case "S" -> "SEARCH";
            // Следующая страница
            case "N" -> "NEXT";
            // Предыдущая страница
            case "P" -> "PREV";
            // Редактировать
            case "E" -> "EDIT";
            // Удалить
            case "D" -> "DELETE";
            default -> "INVALID";
        };
    }

    /**
     * Парсит статус из строки. Поддерживает первую букву (P, A, D, C) или полное слово.
     * Возвращает null, если строка пустая или нераспознаваемая.
     */
    private TaskStatus parseStatus(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        return switch (input) {
            case "P", "PENDING", "pending" -> TaskStatus.PENDING;
            case "A", "ACTIVE", "active" -> TaskStatus.ACTIVE;
            case "D", "DONE", "done" -> TaskStatus.DONE;
            case "C", "CANCEL", "cancel" -> TaskStatus.CANCEL;
            default -> null;
        };
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