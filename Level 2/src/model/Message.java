package model;

/**
 * Basic message
 *
 * Wraps an incoming line of text into a model
 * And can be used to send outgoing messages
 */
public class Message {
    private final Command command;
    private String payload;

    public Message(Command command, String payload) {
        this.command = command;
        this.payload = payload;
    }

    public static Message fromString(String line) {
        String[] split = line.split(" ");
        if (line.isEmpty() || split.length == 0) {
            // Empty line is an unknown command
            return null;
        } else {
            // The command is the first part of the message
            // eg. CONN username
            // Where CONN is the command and username is the payload
            String command = split[0];
            String payload = "";
            if (split.length > 1) {
                payload = line.substring(command.length() + 1);
            }

            // Get command from enumeration
            Command cmd = Command.fromCommand(command);
            if (cmd == null) {
                // If command wasn't found in the enum; it's not a valid command
                return null;
            } else {
                return new Message(cmd, payload);
            }
        }
    }

    @Override
    public String toString() {
        return (command + " " + payload).trim();
    }

    public Command getCommand() {
        return command;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
