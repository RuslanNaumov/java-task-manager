package com.github.ruslannaumov.taskmanager.util;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Утилита для цветного вывода текста в консоль с использованием библиотеки Jansi.
 */
public class ColorUtils {

    public static String green(String text) { return ansi().bold().fg(GREEN).a(text).reset().toString(); }

    public static String yellow(String text) { return ansi().bold().fg(YELLOW).a(text).reset().toString(); }

    public static String blue(String text) { return ansi().bold().fgBright(BLUE).a(text).reset().toString(); }

    public static String bold(String text) { return ansi().bold().a(text).reset().toString(); }

    public static String red(String text) { return ansi().bold().fgBright(RED).a(text).reset().toString(); }
}