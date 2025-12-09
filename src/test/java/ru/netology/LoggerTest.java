package ru.netology;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.netology.common.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest {
    private static final String LOG_FILE = "file.log";
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = new Logger();
        deleteLogFile();
    }

    @AfterEach
    void tearDown() {
        deleteLogFile();
    }

    private void deleteLogFile() {
        File file = new File(LOG_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    @DisplayName("Лог-файл создаётся при записи")
    void testLogCreatesFile() {
        logger.log("TestUser", "Test message");

        File file = new File(LOG_FILE);
        assertTrue(file.exists(), "Лог-файл должен быть создан");
    }

    @Test
    @DisplayName("Лог записывается в корректном формате")
    void testLogFormat() throws IOException {
        logger.log("TestUser", "Test message");

        String content = readLogFile();
        assertTrue(content.contains("TestUser"), "Должно содержать имя");
        assertTrue(content.contains("Test message"), "Должно содержать сообщение");
        assertTrue(content.matches("(?s).*\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}].*"),
                "Должна быть временная метка");
    }

    @Test
    @DisplayName("Системный лог содержит SYSTEM")
    void testLogSystem() throws IOException {
        logger.logSystem("System event");

        String content = readLogFile();
        assertTrue(content.contains("SYSTEM"), "Должно содержать SYSTEM");
        assertTrue(content.contains("System event"), "Должно содержать сообщение");
    }

    @Test
    @DisplayName("Записи добавляются в конец файла")
    void testLogAppends() throws IOException {
        logger.log("User1", "First");
        logger.log("User2", "Second");

        String[] lines = readLogFile().trim().split("\\n");
        assertEquals(2, lines.length, "Должно быть 2 строки");
        assertTrue(lines[0].contains("User1"));
        assertTrue(lines[1].contains("User2"));
    }

    @Test
    @DisplayName("Многопоточная запись работает корректно")
    void testConcurrentLogging() throws InterruptedException, IOException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> logger.log("User" + idx, "Msg" + idx));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        String[] lines = readLogFile().trim().split("\\n");
        assertEquals(threadCount, lines.length);
    }

    private String readLogFile() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
