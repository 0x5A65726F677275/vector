package com.artofvector.log;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Process-wide log sink. UI panels subscribe; backend code stays unaware of Swing.
 */
public final class AppLog {

    public enum Level {
        INFO, WARN, ERROR, DEBUG
    }

    public interface Listener {
        void onLog(Level level, String message);
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private AppLog() {
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void info(String message) {
        emit(Level.INFO, message);
    }

    public static void warn(String message) {
        emit(Level.WARN, message);
    }

    public static void error(String message) {
        emit(Level.ERROR, message);
    }

    public static void error(String message, Throwable error) {
        emit(Level.ERROR, message + ": " + error.getMessage());
    }

    public static void debug(String message) {
        emit(Level.DEBUG, message);
    }

    private static void emit(Level level, String message) {
        String line = "[" + LocalTime.now().format(TIME) + "] " + level + "  " + message;
        System.out.println(line);
        for (Listener listener : LISTENERS) {
            listener.onLog(level, line);
        }
    }
}
