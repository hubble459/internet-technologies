package model;

import util.ServerUtil;

public class Message {
    private final Command command;
    private final String payload;

    public Message(Command command, String payload) {
        this.command = command;
        this.payload = payload;
    }

    public static Message fromLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        } else {
            String[] split = line.split(" ", 2);
            Command command = Command.fromString(split[0]);
            String payload = "";
            if (split.length > 1) {
                payload = split[1];
            }
            return new Message(command, payload);
        }
    }

    public String getPayload() {
        return payload;
    }

    public Command getCommand() {
        return command;
    }

    @Override
    public String toString() {
        if (command == Command.BROADCAST || command == Command.BROADCAST_IN_ROOM || command == Command.WHISPER) {
            String[] split = payload.split(" ", 2);
            String username = split[0];
            StringBuilder message = new StringBuilder();
            if (split.length > 1) {
                message = new StringBuilder(split[1]);
            }

            String color = username.equals(ServerUtil.getUsername()) ? "blue" : "black";
            System.out.println(color);
            return "<html><body style='width: 300px'><strong style='color: " + color + "'>" + username + "</strong><br/>" + message + "</body></html>";
        } else {
            return command + " " + payload;
        }
    }
}
