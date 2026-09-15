package com.github.ruslannaumov.taskmanager.ui;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;

public class JLineInputHelper {

    private static LineReader lineReader;

    public static void init() {
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
            lineReader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
        } catch (IOException e) {
            System.err.println("Failed to initialize JLine: " + e.getMessage());
        }
    }

    /**
     * Читает строку с предзаполненным значением.
     */
    public static String readLineWithDefault(String prompt, String defaultValue) {
        if (lineReader == null) {
            System.err.println("JLine not initialized!");
            return defaultValue;
        }

        // Если значение пустое, просто читаем без предзаполнения
        if (defaultValue == null || defaultValue.isEmpty()) {
            return lineReader.readLine(prompt);
        }

        // Предзаполняем буфер ввода значением по умолчанию
        return lineReader.readLine(prompt, null, defaultValue);
    }

    /**
     * Читает строку без предзаполнения (обычный ввод)
     */
    public static String readLine(String prompt) {
        if (lineReader == null) {
            System.err.println("JLine not initialized!");
            return "";
        }
        return lineReader.readLine(prompt);
    }
}