package helper.model;

import helper.Shared;

public class Message {
    private final boolean isResponse;
    private final boolean isSuccessful;
    private Command command;
    private String payload;

    public Message(Command command, String payload, boolean isResponse, boolean isSuccessful) {
        this.command = command;
        this.payload = payload;
        this.isResponse = isResponse;
        this.isSuccessful = isSuccessful;
    }

    public Message(Command command, String payload) {
        this(command, payload, false, false);
    }

    public static Message fromLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        } else {
            String[] split = line.split(" ", 2);
            String commandString = split[0].toUpperCase();
            boolean isResponse = false;
            boolean isSuccessful = false;
            if (commandString.startsWith("2")) {
                isSuccessful = true;
                isResponse = true;
            } else if (commandString.startsWith("4")) {
                isResponse = true;
            }

            Command command = Command.fromString(commandString);
            String payload = "";
            if (split.length > 1) {
                payload = split[1];
            }
            return new Message(command, payload, isResponse, isSuccessful);
        }
    }

    public boolean isResponse() {
        return isResponse;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String getPayload() {
        return payload;
    }

    public Command getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return command + " " + payload;
    }

    public String toHTML() {
        String payload = this.payload;

        if(command == Command.SERVER) {
            payload = "SERVER " + payload;
        }

        String[] split = payload.split(" ", 2);
        String username = split[0];
        String message = "";
        if (split.length > 1) {
            message = split[1];
        }

        String color = username.equals(Shared.username) ? "blue" : "black";
        if (username.equals("SERVER")) color = "red";
        return "<html><body style='width: 300px'><strong style='font-size: 12px;color: " + color + "'>" + username + "</strong><br/>" + message + "</body></html>";
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
