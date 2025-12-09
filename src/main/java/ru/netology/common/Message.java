package ru.netology.common;

public class Message {
    private final MessageType type;
    private final String username;
    private final String content;

    public enum MessageType {
        JOIN,
        MESSAGE,
        EXIT,
        SYSTEM
    }

    public Message(MessageType type, String username, String content) {
        this.type = type;
        this.username = username;
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }

    public String serialize() {
        return type.name() + ":" + username + ":" + content;
    }

    public static Message deserialize(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] parts = line.split(":", 3);
        if (parts.length < 2) {
            return null;
        }

        try {
            MessageType type = MessageType.valueOf(parts[0]);
            String username = parts[1];
            String content = parts.length > 2 ? parts[2] : "";
            return new Message(type, username, content);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        if (type == MessageType.SYSTEM) {
            return "[СИСТЕМА] " + content;
        }
        return username + ": " + content;
    }
}
