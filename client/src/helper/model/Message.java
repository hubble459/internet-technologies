package helper.model;

import helper.Shared;

/**
 * A basic message to send to the server
 * <p>
 * Always has a Command and can also have a Payload
 */
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

    /**
     * Convert a line of text into a message
     *
     * @param line string
     * @return Message
     */
    public static Message fromLine(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        } else {
            /*
             * Split the line in two parts
             * The first part should aways be a Command or a status code
             * The second part is the payload
             * */
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
            if (command == null) return null;
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

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return command + " " + payload;
    }

    /**
     * Turn this message into HTML format
     * Which could then be used by JFrame to show this message
     *
     * @return this message in HTML format
     */
    public String toHTML() {
        String payload = this.payload;

        if (command == Command.SERVER) {
            payload = "SERVER " + payload;
        }

        String[] split = payload.split(" ", 2);
        String username = split[0];
        String message = "";
        if (split.length > 1) {
            message = split[1];
        }

        // If you send the message, the username will be blue else it will be black
        String color = username.equals(Shared.username) ? "blue" : "black";
        // If it's a notification from server the color will be red
        if (username.equals("SERVER")) color = "red";
        return "<html><body style='width: 250px'><strong style='font-size: 12px;color: " + color + "'>" + username + "</strong><br/>" + message + "</body></html>";
    }
}
