package ui.model;

import java.time.LocalTime;
import java.util.Arrays;

public class Message {
    public static final String CONN = "CONN";
    public static final String BCST = "BCST";
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String QUIT = "QUIT";
    public static final String DCSN = "DCSN";
    public static final String INFO = "INFO";
    public static final String GOOD = "200";
    public static final String BAD1 = "401";
    public static final String BAD2 = "400";

    private final String time;
    private String message;
    private String type;
    private String sender = "SERVER";

    public Message(String sender, String type, String message) {
        this.time = time();
        this.sender = sender;
        this.type = type;
        this.message = message;
    }

    public Message(String[] parts) {
        System.out.println(Arrays.toString(parts));

        time = time();
        type = parts[0];

        if (type.equals(BCST)) {
            sender = parts[1];
            message = message(parts, 2);
        } else {
            message = message(parts, 1);
        }
    }

    private String message(String[] parts, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < parts.length; i++) {
            sb.append(parts[i]).append(" ");
        }
        return sb.toString().trim();
    }

    private String time() {
        return LocalTime.now().toString().split("\\.")[0];
    }

    @Override
    public String toString() {
        return "[" + sender + "] " + message;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTime() {
        return time;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
