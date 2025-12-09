package ru.netology;

import org.junit.jupiter.api.*;
import ru.netology.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatServerTest {
    private static final int TEST_PORT = 8081;
    private static final String TEST_HOST = "localhost";
    private static final String LOG_FILE = "file.log";

    private ChatServer server;

    @BeforeEach
    void setUp() throws InterruptedException {
        deleteLogFile();
        server = new ChatServer(TEST_PORT);
        Thread serverThread = new Thread(() -> server.start());
        serverThread.start();
        Thread.sleep(500);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
        deleteLogFile();
    }

    @Test
    @DisplayName("Сервер принимает подключение клиента")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testServerAcceptsConnection() throws IOException {
        try (Socket socket = new Socket(TEST_HOST, TEST_PORT)) {
            assertTrue(socket.isConnected(), "Клиент должен подключиться к серверу");
        }
    }

    @Test
    @DisplayName("Сервер обрабатывает JOIN сообщение")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testServerHandlesJoin() throws IOException, InterruptedException {
        try (Socket socket = new Socket(TEST_HOST, TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Message join = new Message(Message.MessageType.JOIN, "TestUser", "");
            out.println(join.serialize());

            Thread.sleep(200);

            String response = in.readLine();
            assertNotNull(response, "Должен получить ответ от сервера");
            assertTrue(response.contains("TestUser"), "Ответ должен содержать имя пользователя");
        }
    }

    @Test
    @DisplayName("Сервер рассылает сообщения всем клиентам")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testServerBroadcastsMessages() throws IOException, InterruptedException {
        Socket socket1 = new Socket(TEST_HOST, TEST_PORT);
        PrintWriter out1 = new PrintWriter(socket1.getOutputStream(), true);
        BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

        out1.println(new Message(Message.MessageType.JOIN, "User1", "").serialize());
        Thread.sleep(100);
        in1.readLine();

        Socket socket2 = new Socket(TEST_HOST, TEST_PORT);
        PrintWriter out2 = new PrintWriter(socket2.getOutputStream(), true);
        BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));

        out2.println(new Message(Message.MessageType.JOIN, "User2", "").serialize());
        Thread.sleep(100);

        in1.readLine();
        in2.readLine();

        try {
            out1.println(new Message(Message.MessageType.MESSAGE, "User1", "Привет!").serialize());
            Thread.sleep(100);

            in1.readLine();

            String msg2 = in2.readLine();
            assertNotNull(msg2, "User2 должен получить сообщение");

            Message received = Message.deserialize(msg2);
            assertEquals("User1", received.getUsername());
            assertEquals("Привет!", received.getContent());
        } finally {
            socket1.close();
            socket2.close();
        }
    }

    @Test
    @DisplayName("Сервер обрабатывает EXIT сообщение")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testServerHandlesExit() throws IOException, InterruptedException {
        Socket socket1 = new Socket(TEST_HOST, TEST_PORT);
        PrintWriter out1 = new PrintWriter(socket1.getOutputStream(), true);
        BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));

        out1.println(new Message(Message.MessageType.JOIN, "Alice", "").serialize());
        Thread.sleep(100);
        in1.readLine();

        Socket socket2 = new Socket(TEST_HOST, TEST_PORT);
        PrintWriter out2 = new PrintWriter(socket2.getOutputStream(), true);
        BufferedReader in2 = new BufferedReader(new InputStreamReader(socket2.getInputStream()));

        out2.println(new Message(Message.MessageType.JOIN, "Bob", "").serialize());
        Thread.sleep(100);

        in1.readLine();
        in2.readLine();

        try {
            out1.println(new Message(Message.MessageType.EXIT, "Alice", "").serialize());
            Thread.sleep(100);

            String exitNotification = in2.readLine();
            assertNotNull(exitNotification, "Bob должен получить уведомление");
            assertTrue(exitNotification.contains("Alice"), "Уведомление должно содержать Alice");
        } finally {
            socket1.close();
            socket2.close();
        }
    }

    @Test
    @DisplayName("Сервер логирует сообщения в файл")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testServerLogsMessages() throws IOException, InterruptedException {
        try (Socket socket = new Socket(TEST_HOST, TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(new Message(Message.MessageType.JOIN, "LogTest", "").serialize());
            Thread.sleep(100);
            in.readLine();

            out.println(new Message(Message.MessageType.MESSAGE, "LogTest", "Test message").serialize());
            Thread.sleep(200);

            File logFile = new File(LOG_FILE);
            assertTrue(logFile.exists(), "Лог-файл должен существовать");

            String logContent = readFile(logFile);
            assertTrue(logContent.contains("LogTest"), "Лог должен содержать имя пользователя");
            assertTrue(logContent.contains("Test message"), "Лог должен содержать сообщение");
        }
    }

    @Test
    @DisplayName("Сервер обрабатывает множественные подключения")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @SuppressWarnings("resource")
    void testServerHandlesMultipleClients() throws IOException, InterruptedException {
        int clientCount = 5;
        Socket[] sockets = new Socket[clientCount];
        PrintWriter[] writers = new PrintWriter[clientCount];
        BufferedReader[] readers = new BufferedReader[clientCount];

        try {
            for (int i = 0; i < clientCount; i++) {
                sockets[i] = new Socket(TEST_HOST, TEST_PORT);
                writers[i] = new PrintWriter(sockets[i].getOutputStream(), true);
                readers[i] = new BufferedReader(new InputStreamReader(sockets[i].getInputStream()));

                writers[i].println(new Message(Message.MessageType.JOIN, "User" + i, "").serialize());
                Thread.sleep(100);

                assertTrue(sockets[i].isConnected(), "Клиент " + i + " должен быть подключен");
            }

            for (int i = 0; i < clientCount; i++) {
                int expectedMessages = clientCount - i;
                for (int j = 0; j < expectedMessages; j++) {
                    String msg = readers[i].readLine();
                    assertNotNull(msg, "User" + i + " должен получить сообщение");
                }
            }
        } finally {
            for (Socket socket : sockets) {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }
    }

    private void deleteLogFile() {
        File file = new File(LOG_FILE);
        if (file.exists()) {
            file.delete();
        }
    }

    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}
