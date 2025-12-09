package ru.netology.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {
    private static final String SETTINGS_FILE = "settings.txt";
    private static final int DEFAULT_PORT = 8080;

    private final Properties properties;

    public Settings() throws IOException {
        properties = new Properties();
        loadSettings();
    }

    private void loadSettings() throws IOException {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(SETTINGS_FILE)) {
            if (inputStream == null) {
                throw new IOException("Файл настроек не найден: " + SETTINGS_FILE);
            }
            properties.load(inputStream);
        }
    }

    public int getPort() {
        String portStr = properties.getProperty("port", String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.err.println("Неверный формат порта: " + portStr + ". Используется: " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    @Override
    public String toString() {
        return "Settings{port=" + getPort() + "}";
    }
}
