package ru.netology;

import ru.netology.common.Logger;
import ru.netology.common.Message;
import ru.netology.common.Settings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private final int port;
    private final List<ClientHandler> clients;
    private final Logger logger;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private volatile boolean stopped;

    public ChatServer(int port) {
        this.port = port;
        this.clients = new CopyOnWriteArrayList<>();
        this.logger = new Logger();
        this.running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Сервер запущен на порту " + port);
            logger.logSystem("Сервер запущен на порту " + port);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Новое подключение: " + clientSocket.getInetAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);

                    new Thread(clientHandler).start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Ошибка при подключении клиента: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Не удалось запустить сервер: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public synchronized void stop() {
        if (stopped) {
            return;
        }
        stopped = true;

        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.disconnect();
            }
            clients.clear();
            System.out.println("Сервер остановлен");
            logger.logSystem("Сервер остановлен");
        } catch (IOException e) {
            System.err.println("Ошибка при остановке сервера: " + e.getMessage());
        }
    }

    public void broadcast(Message message) {
        if (message.getType() == Message.MessageType.SYSTEM) {
            logger.logSystem(message.getContent());
        } else {
            logger.log(message.getUsername(), message.getContent());
        }

        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    public static void main(String[] args) {
        try {
            Settings settings = new Settings();
            int port = settings.getPort();

            ChatServer server = new ChatServer(port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nОстановка сервера...");
                server.stop();
            }));

            server.start();
        } catch (IOException e) {
            System.err.println("Ошибка при чтении настроек: " + e.getMessage());
            System.exit(1);
        }
    }
}
