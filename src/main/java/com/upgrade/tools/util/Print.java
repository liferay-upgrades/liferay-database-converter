package com.upgrade.tools.util;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * @author Albett Gomes Cabral
 */
public class Print {

    public static void debug(String message, Object... args) {
        if (!_DEBUG_ENABLED) {
            return;
        }

        _log(System.out, _LABEL_DEBUG, _COLOR_GRAY, message, args);
    }

    public static void error(String message, Object... args) {
        _log(System.err, _LABEL_ERROR, _COLOR_RED, message, args);
    }

    public static void error(String message, Throwable cause) {
        _log(System.err, _LABEL_ERROR, _COLOR_RED, message);

        if (cause != null) {
            cause.printStackTrace(System.err);
        }
    }

    public static void info(String message, Object... args) {
        _log(System.out, _LABEL_INFO, _COLOR_GREEN, message, args);
    }

    public static void replacement(
            String oldContent, String newContent, Pattern pattern) {

        info("Applying pattern: %s", pattern.pattern());

        System.out.println(
                "  " + _colorize(_COLOR_LIGHT_BLUE, "- " + oldContent));
        System.out.println(
                "  " + _colorize(_COLOR_GREEN, "+ " + newContent));
        System.out.println();
    }

    public static void warn(String message, Object... args) {
        _log(System.err, _LABEL_WARN, _COLOR_YELLOW, message, args);
    }

    public static void warn(String message, Throwable cause) {
        _log(System.err, _LABEL_WARN, _COLOR_YELLOW, message);

        if (cause != null) {
            cause.printStackTrace(System.err);
        }
    }

    private Print() {
    }

    private static String _colorize(String color, String text) {
        if (!_COLOR_ENABLED) {
            return text;
        }

        return color + text + "\u001B[0m";
    }

    private static boolean _detectColorSupport() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }

        if (System.getenv("FORCE_COLOR") != null) {
            return true;
        }

        return System.console() != null;
    }

    private static void _log(
        PrintStream stream, String label, String color, String message,
        Object... args) {

        String formatted = (args == null || args.length == 0) ?
            message : String.format(message, args);

        String timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        stream.println(
            timestamp + " " + _colorize(color, label) + " " + formatted);
    }

    private static final boolean _COLOR_ENABLED = _detectColorSupport();

    private static final String _COLOR_GRAY = "\u001B[90m";

    private static final String _COLOR_GREEN = "\u001B[32m";

    private static final String _COLOR_LIGHT_BLUE = "\u001B[94m";

    private static final String _COLOR_RED = "\u001B[31m";

    private static final String _COLOR_YELLOW = "\u001B[33m";

    private static final boolean _DEBUG_ENABLED = Boolean.parseBoolean(
        System.getenv().getOrDefault("CONVERTER_DEBUG", "false"));

    private static final String _LABEL_DEBUG = "[DEBUG]";

    private static final String _LABEL_ERROR = "[ERROR]";

    private static final String _LABEL_INFO  = "[INFO]";

    private static final String _LABEL_WARN  = "[WARN]";

}
