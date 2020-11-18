package server;

public class Message {
    private final Command command;
    private String payload;

    public Message(Command command, String payload) {
        this.command = command;
        this.payload = payload;
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
