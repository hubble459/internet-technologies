package server;

public enum Command {
    INFO("INFO"),
    JOINED("JOIN"),
    ROOMS("ROOMS"),
    JOIN_ROOM("ROOM"),
    LOGIN("CONN"),
    QUIT("QUIT"),
    BROADCAST("BCST"),
    PING("PING"),
    PONG("PONG"),
    DISCONNECTED("DCST"), // DSCT?!
    UNKNOWN("400"),
    ALREADY_LOGGED_IN("401"),
    INVALID_FORMAT("402"),
    LOGGED_IN("200"),
    QUITED("201");

    private final String command;

    Command(String command) {
        this.command = command;
    }

    @Override
    public String toString() {
        return command;
    }

    public static Command fromCommand(String command) {
        for (Command value : values()) {
            if (value.command.equals(command)) {
                return value;
            }
        }
        return null;
    }
}
