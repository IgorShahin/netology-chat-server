package ru.netology.common;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE = "file.log";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public synchronized void log(String username, String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("[%s] %s: %s", timestamp, username, message);
        writeToFile(logEntry);
    }

    public synchronized void logSystem(String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String logEntry = String.format("[%s] SYSTEM: %s", timestamp, message);
        writeToFile(logEntry);
    }

    private void writeToFile(String logEntry) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            System.err.println("Ошибка записи в лог-файл: " + e.getMessage());
        }
    }
}
