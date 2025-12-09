package ru.netology;

import ru.netology.common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private volatile boolean connected;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while (connected && (line = in.readLine()) != null) {
                Message message = Message.deserialize(line);

                if (message == null) {
                    continue;
                }

                switch (message.getType()) {
                    case JOIN:
                        handleJoin(message);
                        break;
                    case MESSAGE:
                        handleMessage(message);
                        break;
                    case EXIT:
                        handleExit(message);
                        return;
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("Ошибка при обработке клиента: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void handleJoin(Message message) {
        username = message.getUsername();
        System.out.println(username + " присоединился к чату");

        Message systemMessage = new Message(
                Message.MessageType.SYSTEM,
                "SYSTEM",
                username + " присоединился к чату"
        );
        server.broadcast(systemMessage);
    }

    private void handleMessage(Message message) {
        System.out.println(message.getUsername() + ": " + message.getContent());
        server.broadcast(message);
    }

    private void handleExit(Message message) {
        System.out.println(message.getUsername() + " покинул чат");

        Message systemMessage = new Message(
                Message.MessageType.SYSTEM,
                "SYSTEM",
                message.getUsername() + " покинул чат"
        );
        server.broadcast(systemMessage);

        disconnect();
    }

    public void sendMessage(Message message) {
        if (out != null && connected) {
            out.println(message.serialize());
        }
    }

    public void disconnect() {
        connected = false;
        server.removeClient(this);

        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }
}
